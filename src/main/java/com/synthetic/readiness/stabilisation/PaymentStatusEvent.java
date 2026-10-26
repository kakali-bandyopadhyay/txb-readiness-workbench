package com.synthetic.readiness.stabilisation;

import java.time.LocalDateTime;

public record PaymentStatusEvent(String eventId, long releaseId, String paymentReference, String status, LocalDateTime occurredAt, String journeyStep, Boolean synthetic) {
    public PaymentStatusEvent { if (synthetic == null) synthetic = true; }
}

