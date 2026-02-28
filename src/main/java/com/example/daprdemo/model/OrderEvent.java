package com.example.daprdemo.model;

import java.time.Instant;

public record OrderEvent(
    String eventId,
    String orderId,
    String eventType,
    Instant timestamp
) {
    public static OrderEvent created(String orderId) {
        return new OrderEvent(
            java.util.UUID.randomUUID().toString(),
            orderId,
            "ORDER_CREATED",
            Instant.now()
        );
    }
}
