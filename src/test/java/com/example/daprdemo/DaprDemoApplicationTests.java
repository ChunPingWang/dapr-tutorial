package com.example.daprdemo;

import com.example.daprdemo.repository.OrderRepository;
import io.dapr.client.DaprClient;
import io.dapr.spring.data.KeyValueAdapterResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class DaprDemoApplicationTests {

    @MockitoBean
    private DaprClient daprClient;

    @MockitoBean
    private KeyValueAdapterResolver keyValueAdapterResolver;

    @MockitoBean
    private OrderRepository orderRepository;

    @Test
    void contextLoads() {
        // 驗證 Spring Boot 應用能正常啟動
        // Mock Dapr 相關的 Bean，因為本測試不需要真正的 Dapr sidecar
    }
}
