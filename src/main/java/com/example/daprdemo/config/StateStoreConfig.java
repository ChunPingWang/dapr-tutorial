package com.example.daprdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.statestore.DaprStateStoreProperties;
import io.dapr.spring.data.DaprKeyValueAdapterResolver;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.spring.data.KeyValueAdapterResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DaprStateStoreProperties.class})
public class StateStoreConfig {

    @Bean
    public KeyValueAdapterResolver keyValueAdapterResolver(
            DaprClient daprClient,
            ObjectMapper mapper,
            DaprStateStoreProperties properties) {
        return new DaprKeyValueAdapterResolver(
            daprClient, mapper,
            properties.getName(),
            properties.getBinding()
        );
    }

    @Bean
    public DaprKeyValueTemplate daprKeyValueTemplate(
            KeyValueAdapterResolver resolver) {
        return new DaprKeyValueTemplate(resolver);
    }
}
