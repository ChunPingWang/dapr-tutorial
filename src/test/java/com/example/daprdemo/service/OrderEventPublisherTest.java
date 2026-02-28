package com.example.daprdemo.service;

import com.example.daprdemo.model.OrderEvent;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private DaprMessagingTemplate<OrderEvent> messagingTemplate;

    @InjectMocks
    private OrderEventPublisher publisher;

    @Test
    void publishOrderCreated_應發送正確的事件() {
        // Act
        publisher.publishOrderCreated("order-123");

        // Assert
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(messagingTemplate).send(eq("orders"), captor.capture());

        OrderEvent event = captor.getValue();
        assertEquals("order-123", event.orderId());
        assertEquals("ORDER_CREATED", event.eventType());
        assertNotNull(event.eventId());
        assertNotNull(event.timestamp());
    }
}
