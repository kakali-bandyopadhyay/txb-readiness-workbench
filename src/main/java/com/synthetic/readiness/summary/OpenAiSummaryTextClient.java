package com.synthetic.readiness.summary;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
class OpenAiSummaryTextClient implements SummaryTextClient {
    private final ObjectMapper json;
    private final String apiKey;
    OpenAiSummaryTextClient(ObjectMapper json, @Value("${OPENAI_API_KEY:}") String apiKey) { this.json = json; this.apiKey = apiKey; }
    public String summarize(String facts) throws Exception {
        if (apiKey.isBlank()) throw new IllegalStateException("Model mode needs OPENAI_API_KEY in the environment.");
        String payload = json.writeValueAsString(Map.of("model", "gpt-4.1-mini", "input", "Write a concise executive readiness summary from these dashboard facts only. Do not infer missing facts. Clearly state this is a draft. Facts: " + facts));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
        String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode root = json.readTree(response);
        for (JsonNode item : root.path("output")) for (JsonNode content : item.path("content")) if (content.has("text")) return content.path("text").asText();
        throw new IllegalStateException("Model response did not include summary text.");
    }
}
