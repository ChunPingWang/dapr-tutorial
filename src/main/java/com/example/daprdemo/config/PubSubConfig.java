package com.example.daprdemo.config;

import com.example.daprdemo.model.OrderEvent;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DaprPubSubProperties.class})
public class PubSubConfig {

    @Bean
    public DaprMessagingTemplate<OrderEvent> orderEventMessagingTemplate(
            DaprClient daprClient,
            DaprPubSubProperties properties) {
        return new DaprMessagingTemplate<>(daprClient, properties.getName(), false);
    }
}
