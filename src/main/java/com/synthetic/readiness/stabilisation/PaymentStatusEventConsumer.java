package com.synthetic.readiness.stabilisation;

import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusEventConsumer {
    private final StabilisationService stabilisation;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public PaymentStatusEventConsumer(StabilisationService stabilisation, KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.stabilisation = stabilisation;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = "payment-status-events", groupId = "readiness-workbench")
    public void accept(String payload) throws Exception {
        PaymentStatusEvent event = json.readValue(payload, PaymentStatusEvent.class);
        if (!Boolean.TRUE.equals(event.synthetic())) throw new IllegalArgumentException("Only synthetic payment-status events are accepted.");
        if (!"FAILED".equalsIgnoreCase(event.status()) && !"REJECTED".equalsIgnoreCase(event.status())) return;
        StabilisationException exception = stabilisation.route(event);
        kafka.send("stabilisation-exceptions", exception.eventId, json.writeValueAsString(exception));
    }
}
