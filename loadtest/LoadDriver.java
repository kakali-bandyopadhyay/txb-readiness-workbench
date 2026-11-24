import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Closed-loop HTTP load driver for the A2 server-thread comparison. */
public class LoadDriver {
    private static final String CSV_HEADER = "label,concurrency,vt,sessions,requests,throughput_req_s,p50_ms,p95_ms,p99_ms,errors,409s,max_hikaricp_pending";
    private static final String ACTIONS_PATH = "/api/releases/1/actions";
    private static final String DASHBOARD_PATH = "/api/releases/1/dashboard";
    private static final String PENDING_METRIC_PATH = "/actuator/metrics/hikaricp.connections.pending";
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\\"value\\\"\\s*:\\s*([-+0-9.eE]+)");
    private static final long RANDOM_SEED = 0x545842A2L;

    private record Config(String base, int concurrency, int warmupSeconds, int durationSeconds,
                          String label, Path outputDirectory, boolean serverVirtualThreads, boolean sessions) {}

    private static final class Results {
        final ConcurrentLinkedQueue<Long> latencyNanos = new ConcurrentLinkedQueue<>();
        final LongAdder requests = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder conflicts = new LongAdder();

        void record(long startedNanos, long measuredStartNanos, long elapsedNanos, int status, boolean exception) {
            if (startedNanos < measuredStartNanos) return;
            latencyNanos.add(elapsedNanos);
            requests.increment();
            if (exception || status < 200 || status >= 300) errors.increment();
            if (status == 409) conflicts.increment();
        }
    }

    public static void main(String[] args) throws Exception {
        Config config = parseArgs(args);
        String base = trimTrailingSlash(config.base());
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
            "ops:demo-ops".getBytes(StandardCharsets.UTF_8));
        List<Long> actionIds = loadActionIds(newHttpClient(newCookieManager(true)), base, authorization);
        AtomicInteger actionCursor = new AtomicInteger();
        Results results = new Results();
        AtomicInteger maxPending = new AtomicInteger();
        long[] measurementWindow = new long[2];
        CountDownLatch ready = new CountDownLatch(config.concurrency());
        CountDownLatch start = new CountDownLatch(1);

        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>(config.concurrency());
            for (int workerId = 0; workerId < config.concurrency(); workerId++) {
                final int id = workerId;
                futures.add(workers.submit(() -> runWorker(id, base, authorization, actionIds,
                    actionCursor, results, ready, start, measurementWindow, config.sessions())));
            }

            if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not become ready");
            long runStartedNanos = System.nanoTime();
            measurementWindow[0] = runStartedNanos + TimeUnit.SECONDS.toNanos(config.warmupSeconds());
            measurementWindow[1] = measurementWindow[0] + TimeUnit.SECONDS.toNanos(config.durationSeconds());
            start.countDown();
            Thread poolWatcher = Thread.ofVirtual().name("hikari-pending-watcher").start(
                () -> watchPending(newHttpClient(newCookieManager(true)), base, authorization, measurementWindow, maxPending));
            for (var future : futures) future.get();
            poolWatcher.join();
        }

        String row = formatCsv(config, results, maxPending.get());
        appendCsv(config.outputDirectory(), row);
        System.out.println(row);
    }

    private static void runWorker(int workerId, String base, String authorization,
                                  List<Long> actionIds, AtomicInteger actionCursor, Results results,
                                  CountDownLatch ready, CountDownLatch start, long[] measurementWindow, boolean sessions) {
        CookieManager cookies = newCookieManager(sessions);
        HttpClient client = newHttpClient(cookies);
        SplittableRandom random = new SplittableRandom(RANDOM_SEED + workerId);
        boolean sessionEstablished = false;
        ready.countDown();
        try {
            start.await();
            long measuredStartNanos = measurementWindow[0];
            long measuredEndNanos = measurementWindow[1];
            while (System.nanoTime() < measuredEndNanos) {
                long started = System.nanoTime();
                int status = 0;
                boolean exception = false;
                boolean stopWorker = false;
                try {
                    HttpRequest request;
                    if (random.nextInt(100) < 80) {
                        request = request(base + DASHBOARD_PATH, authorization, !sessions || !sessionEstablished).GET().build();
                    } else {
                        int index = Math.floorMod(actionCursor.getAndIncrement(), actionIds.size());
                        long actionId = actionIds.get(index);
                        request = request(base + "/api/actions/" + actionId, authorization, !sessions || !sessionEstablished)
                            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"owner\":\"Load Test\"}"))
                            .header("Content-Type", "application/json")
                            .build();
                    }
                    status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                    if (sessions && !sessionEstablished && status >= 200 && status < 300) {
                        sessionEstablished = hasSessionCookie(cookies);
                        if (!sessionEstablished) {
                            exception = true;
                            stopWorker = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exception = true;
                } catch (Exception e) {
                    exception = true;
                }
                long elapsed = System.nanoTime() - started;
                results.record(started, measuredStartNanos, elapsed, status, exception);
                if (stopWorker || Thread.currentThread().isInterrupted()) return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void watchPending(HttpClient client, String base, String authorization,
                                     long[] measurementWindow,
                                     AtomicInteger maxPending) {
        long measuredStartNanos = measurementWindow[0];
        long measuredEndNanos = measurementWindow[1];
        while (System.nanoTime() < measuredEndNanos) {
            long now = System.nanoTime();
            try {
                HttpRequest request = request(base + PENDING_METRIC_PATH, authorization, true).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (now >= measuredStartNanos && response.statusCode() >= 200 && response.statusCode() < 300) {
                    Matcher matcher = VALUE_PATTERN.matcher(response.body());
                    if (matcher.find()) maxPending.accumulateAndGet((int) Math.ceil(Double.parseDouble(matcher.group(1))), Math::max);
                }
            } catch (Exception ignored) {
                // The watcher is auxiliary; request errors are measured by the workers.
            }
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static List<Long> loadActionIds(HttpClient client, String base, String authorization) throws Exception {
        HttpRequest request = request(base + ACTIONS_PATH, authorization, true).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Could not load seeded action ids; status=" + response.statusCode());
        }
        Matcher matcher = ID_PATTERN.matcher(response.body());
        List<Long> ids = new ArrayList<>();
        while (matcher.find()) ids.add(Long.parseLong(matcher.group(1)));
        if (ids.isEmpty()) throw new IOException("Release 1 returned no action ids");
        return List.copyOf(ids);
    }

    private static CookieManager newCookieManager(boolean acceptCookies) {
        return new CookieManager(null, acceptCookies ? CookiePolicy.ACCEPT_ALL : CookiePolicy.ACCEPT_NONE);
    }

    private static HttpClient newHttpClient(CookieManager cookieManager) {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .cookieHandler(cookieManager)
            .build();
    }

    private static boolean hasSessionCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream().anyMatch(cookie -> "JSESSIONID".equalsIgnoreCase(cookie.getName()));
    }

    private static HttpRequest.Builder request(String uri, String authorization, boolean includeBasic) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json");
        if (includeBasic) builder.header("Authorization", authorization);
        return builder;
    }

    private static String formatCsv(Config config, Results results, int maxPending) {
        long[] sorted = results.latencyNanos.stream().mapToLong(Long::longValue).sorted().toArray();
        double seconds = config.durationSeconds();
        return String.format(Locale.ROOT, "%s,%d,%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%d,%d,%d",
            csv(config.label()), config.concurrency(), config.serverVirtualThreads(), config.sessions(), results.requests.sum(),
            results.requests.sum() / seconds, percentileMs(sorted, 0.50), percentileMs(sorted, 0.95),
            percentileMs(sorted, 0.99), results.errors.sum(), results.conflicts.sum(), maxPending);
    }

    private static double percentileMs(long[] sorted, double percentile) {
        if (sorted.length == 0) return 0.0;
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index] / 1_000_000.0;
    }

    private static void appendCsv(Path outputDirectory, String row) throws IOException {
        Files.createDirectories(outputDirectory);
        Path output = outputDirectory.resolve("results.csv");
        boolean needsHeader = !Files.exists(output) || Files.size(output) == 0;
        try (var writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needsHeader) writer.write(CSV_HEADER + System.lineSeparator());
            writer.write(row + System.lineSeparator());
        }
    }

    private static Config parseArgs(String[] args) {
        Map<String, String> values = new java.util.HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length || !args[i].startsWith("--")) throw new IllegalArgumentException("Expected --name value arguments");
            values.put(args[i].substring(2), args[i + 1]);
        }
        String label = required(values, "label");
        Matcher mode = Pattern.compile("(?:^|-)vt-(on|off)(?:-|$)").matcher(label);
        if (!mode.find()) throw new IllegalArgumentException("Label must include vt-on or vt-off, for example vt-off-c50-r1");
        String sessionSetting = required(values, "sessions");
        if (!sessionSetting.equalsIgnoreCase("true") && !sessionSetting.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("--sessions must be true or false");
        return new Config(required(values, "base"), positive(values, "concurrency"), nonNegative(values, "warmup"),
            positive(values, "duration"), label, Path.of(required(values, "out")), "on".equals(mode.group(1)),
            Boolean.parseBoolean(sessionSetting));
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing --" + key);
        return value;
    }

    private static int positive(Map<String, String> values, String key) {
        int value = Integer.parseInt(required(values, key));
        if (value < 1) throw new IllegalArgumentException("--" + key + " must be positive");
        return value;
    }

    private static int nonNegative(Map<String, String> values, String key) {
        int value = Integer.parseInt(required(values, key));
        if (value < 0) throw new IllegalArgumentException("--" + key + " must not be negative");
        return value;
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String csv(String value) {
        return value.contains(",") || value.contains("\"")
            ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
