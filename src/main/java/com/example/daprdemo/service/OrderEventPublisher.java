package com.example.daprdemo.service;

import com.example.daprdemo.model.OrderEvent;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String TOPIC_NAME = "orders";

    private final DaprMessagingTemplate<OrderEvent> messagingTemplate;

    public OrderEventPublisher(DaprMessagingTemplate<OrderEvent> messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderCreated(String orderId) {
        OrderEvent event = OrderEvent.created(orderId);
        messagingTemplate.send(TOPIC_NAME, event);
        log.info("已發布訂單建立事件: orderId={}", orderId);
    }
}
