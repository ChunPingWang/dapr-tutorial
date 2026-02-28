package com.example.daprdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.statestore.DaprStateStoreProperties;
import io.dapr.spring.data.DaprKeyValueAdapterResolver;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.spring.data.KeyValueAdapterResolver;
import io.dapr.spring.data.repository.config.EnableDaprRepositories;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DaprStateStoreProperties.class})
@EnableDaprRepositories(basePackages = "com.example.daprdemo.repository")
public class StateStoreConfig {

    @Bean
    public KeyValueAdapterResolver keyValueAdapterResolver(
            DaprClient daprClient,
            DaprStateStoreProperties properties) {
        // Dapr SDK 1.16 使用 Jackson 2（com.fasterxml），
        // Spring Boot 4 預設使用 Jackson 3（tools.jackson），
        // 這裡手動建立 Jackson 2 ObjectMapper 給 Dapr 使用
        ObjectMapper mapper = new ObjectMapper();
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
