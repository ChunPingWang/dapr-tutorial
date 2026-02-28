package com.example.daprdemo.controller;

import com.example.daprdemo.model.OrderEvent;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(OrderEventSubscriber.class);

    @PostMapping("/events/orders")
    @Topic(pubsubName = "pubsub", name = "orders")
    public void handleOrderEvent(@RequestBody CloudEvent<OrderEvent> cloudEvent) {
        OrderEvent event = cloudEvent.getData();
        log.info("收到訂單事件: eventType={}, orderId={}",
            event.eventType(), event.orderId());
    }
}
