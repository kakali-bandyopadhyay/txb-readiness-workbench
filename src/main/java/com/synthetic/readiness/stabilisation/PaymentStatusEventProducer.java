package com.synthetic.readiness.stabilisation;

import com.synthetic.readiness.readiness.WorkbenchService;
import com.synthetic.readiness.release.ReleaseRecord;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "demo.events.enabled", havingValue = "true", matchIfMissing = false)
public class PaymentStatusEventProducer {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final WorkbenchService workbench;
    private final String[] states = {"ACCEPTED", "ACCEPTED", "FAILED", "REJECTED", "ACCEPTED"};
    private int sequence;

    public PaymentStatusEventProducer(KafkaTemplate<String, String> kafka, ObjectMapper json, WorkbenchService workbench) {
        this.kafka = kafka;
        this.json = json;
        this.workbench = workbench;
    }

    @Scheduled(fixedRateString = "${demo.events.interval-ms:1000}")
    public void publishAfterRecordedGoLive() {
        ReleaseRecord release = workbench.release(1);
        if (release == null || !"LIVE".equalsIgnoreCase(release.lifecycleStatus)) return;
        try {
            String status = states[(sequence++) % states.length];
            PaymentStatusEvent event = new PaymentStatusEvent(UUID.randomUUID().toString(), release.id,
                "SYNTH-PAY-%05d".formatted(sequence), status, LocalDateTime.now(), "Payment status review", true);
            kafka.send("payment-status-events", event.paymentReference(), json.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("Could not publish synthetic payment status event", e);
        }
    }
}
