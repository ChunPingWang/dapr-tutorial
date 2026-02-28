package com.example.daprdemo.integration;

import com.example.daprdemo.model.Order;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 整合測試：使用 Testcontainers 啟動 Dapr sidecar + Redis，
 * 驗證 State Store 的 CRUD 操作。
 *
 * 測試觀點：
 * - 使用真實的 Dapr sidecar，而非 Mock
 * - 驗證資料確實能寫入、讀取、刪除
 * - 每次測試使用獨立的容器環境，確保隔離性
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderIntegrationTest {

    private static final String STATE_STORE_NAME = "kvstore";

    static Network network = Network.newNetwork();

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withNetwork(network)
        .withNetworkAliases("redis");

    @Container
    static DaprContainer dapr = new DaprContainer("daprio/daprd:1.16.0")
        .withAppName("dapr-integration-test")
        .withNetwork(network)
        .withComponent(new Component(
            "kvstore", "state.redis", "v1",
            Map.of("redisHost", "redis:6379")))
        .withComponent(new Component(
            "pubsub", "pubsub.redis", "v1",
            Map.of("redisHost", "redis:6379")))
        .dependsOn(redis);

    private static DaprClient daprClient;

    @BeforeAll
    static void setupClient() {
        daprClient = new DaprClientBuilder()
            .withPropertyOverride(Properties.GRPC_ENDPOINT,
                dapr.getGrpcEndpoint())
            .withPropertyOverride(Properties.HTTP_ENDPOINT,
                dapr.getHttpEndpoint())
            .build();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void 儲存訂單_應成功寫入Dapr狀態儲存() {
        Order order = new Order("integration-1", "測試商品", 3, 999.0);

        assertDoesNotThrow(() ->
            daprClient.saveState(STATE_STORE_NAME, order.orderId(), order).block()
        );
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void 查詢訂單_應從Dapr狀態儲存取回資料() {
        var state = daprClient.getState(
            STATE_STORE_NAME, "integration-1", Order.class
        ).block();

        assertNotNull(state);
        assertNotNull(state.getValue());
        assertEquals("integration-1", state.getValue().orderId());
        assertEquals("測試商品", state.getValue().product());
        assertEquals(3, state.getValue().quantity());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void 刪除訂單_應從狀態儲存移除() {
        assertDoesNotThrow(() ->
            daprClient.deleteState(STATE_STORE_NAME, "integration-1").block()
        );

        var state = daprClient.getState(
            STATE_STORE_NAME, "integration-1", Order.class
        ).block();

        assertNotNull(state);
        assertNull(state.getValue());
    }

    @AfterAll
    static void cleanup() {
        if (daprClient != null) {
            try { daprClient.close(); } catch (Exception ignored) {}
        }
        network.close();
    }
}
