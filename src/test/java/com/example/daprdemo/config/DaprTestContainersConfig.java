package com.example.daprdemo.config;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

    @Bean
    public Network daprNetwork() {
        return Network.newNetwork();
    }

    @Bean
    public GenericContainer<?> redisContainer(Network daprNetwork) {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withNetwork(daprNetwork)
            .withNetworkAliases("redis");
    }

    @Bean
    @ServiceConnection
    public DaprContainer daprContainer(
            Network daprNetwork,
            GenericContainer<?> redisContainer) {
        return new DaprContainer("daprio/daprd:1.16.0")
            .withAppName("dapr-demo")
            .withNetwork(daprNetwork)
            .withComponent(new Component(
                "kvstore", "state.redis", "v1",
                Map.of("redisHost", "redis:6379")))
            .withComponent(new Component(
                "pubsub", "pubsub.redis", "v1",
                Map.of("redisHost", "redis:6379")))
            .withAppPort(8080)
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(redisContainer);
    }
}
