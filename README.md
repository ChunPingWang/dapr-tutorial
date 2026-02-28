# Dapr + Java 21 + Spring Boot 4 教學指南

> 本教學帶領初學者從零開始，使用 Dapr、Java 21 與 Spring Boot 4 建立分散式微服務應用，
> 並以**測試驅動**的方式驗證每一個階段的成果。

## 目錄

- [前置條件](#前置條件)
- [階段一：專案初始化與環境驗證](#階段一專案初始化與環境驗證)
- [階段二：State Store（狀態管理）](#階段二state-store狀態管理)
- [階段三：Pub/Sub（發布訂閱）](#階段三pubsub發布訂閱)
- [階段四：整合測試（Testcontainers）](#階段四整合測試testcontainers)
- [階段五：使用 Dapr CLI 運行完整應用](#階段五使用-dapr-cli-運行完整應用)
- [常見問題](#常見問題)
- [參考資源](#參考資源)

---

## 前置條件

在開始之前，請確認已安裝以下工具：

| 工具 | 最低版本 | 用途 |
|------|---------|------|
| **JDK** | 21 | Java 開發環境（LTS） |
| **Maven** | 3.9+ | 建置工具 |
| **Docker** | 24+ | 容器化運行環境（Dapr sidecar、Testcontainers） |
| **Dapr CLI** | 1.16+ | Dapr 命令列工具 |
| **Git** | 2.x | 版本控制 |

### 安裝 Dapr CLI

```bash
# Linux / macOS
wget -q https://raw.githubusercontent.com/dapr/cli/master/install/install.sh -O - | /bin/bash

# macOS（Homebrew）
brew install dapr/tap/dapr-cli

# Windows（PowerShell）
powershell -Command "iwr -useb https://raw.githubusercontent.com/dapr/cli/master/install/install.ps1 | iex"

# Windows（winget）
winget install Dapr.CLI
```

### 初始化 Dapr

```bash
dapr init
```

此命令會在本機安裝 Dapr sidecar 執行檔，並啟動 Redis 容器作為預設的 State Store 與 Pub/Sub 元件。

### 驗證安裝

```bash
# 確認 Java 版本
java -version
# 預期輸出：openjdk version "21.x.x"

# 確認 Dapr CLI
dapr --version
# 預期輸出：CLI version: 1.16.x / Runtime version: 1.16.x

# 確認 Docker 運行中
docker ps
# 預期看到 dapr_redis、dapr_zipkin 等容器
```

> **測試觀點**：每一項前置條件都應獨立驗證，避免後續除錯時不確定問題來源。

---

## 階段一：專案初始化與環境驗證

### 1.1 建立 Spring Boot 4 專案

建立以下 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.3</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>dapr-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>dapr-demo</name>
    <description>Dapr + Java 21 + Spring Boot 4 示範專案</description>

    <properties>
        <java.version>21</java.version>
        <dapr.version>1.16.0</dapr.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Dapr Spring Boot Starter -->
        <dependency>
            <groupId>io.dapr.spring</groupId>
            <artifactId>dapr-spring-boot-starter</artifactId>
            <version>${dapr.version}</version>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Dapr Testcontainers 支援 -->
        <dependency>
            <groupId>io.dapr.spring</groupId>
            <artifactId>dapr-spring-boot-starter-test</artifactId>
            <version>${dapr.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.2 建立主程式

```java
// src/main/java/com/example/daprdemo/DaprDemoApplication.java
package com.example.daprdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DaprDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DaprDemoApplication.class, args);
    }
}
```

### 1.3 建立健康檢查端點

```java
// src/main/java/com/example/daprdemo/controller/HealthController.java
package com.example.daprdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "java.version", System.getProperty("java.version"),
            "service", "dapr-demo"
        );
    }
}
```

### 1.4 設定檔

```properties
# src/main/resources/application.properties
spring.application.name=dapr-demo
server.port=8080
```

### 1.5 測試：驗證應用能啟動

```java
// src/test/java/com/example/daprdemo/DaprDemoApplicationTests.java
package com.example.daprdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DaprDemoApplicationTests {

    @Test
    void contextLoads() {
        // 驗證 Spring Boot 應用能正常啟動
        // 若此測試失敗，代表 pom.xml 或主程式設定有問題
    }
}
```

```java
// src/test/java/com/example/daprdemo/controller/HealthControllerTest.java
package com.example.daprdemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("dapr-demo"));
    }

    @Test
    void healthEndpointReturnsJavaVersion() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.['java.version']").isNotEmpty());
    }
}
```

### 1.6 執行測試

```bash
mvn test
```

> **測試觀點**：先確認「不含 Dapr」的基本功能正常，才能確保後續加入 Dapr 時，
> 若出現問題，一定是 Dapr 整合造成的，而非基礎設定的問題。這就是**隔離變因**的概念。

### 提交本階段

```bash
git add pom.xml \
  src/main/java/com/example/daprdemo/DaprDemoApplication.java \
  src/main/java/com/example/daprdemo/controller/HealthController.java \
  src/main/resources/application.properties \
  src/test/java/com/example/daprdemo/DaprDemoApplicationTests.java \
  src/test/java/com/example/daprdemo/controller/HealthControllerTest.java

git commit -m "feat: 初始化 Spring Boot 4 + Java 21 專案結構與健康檢查端點"
git push origin main
```

---

## 階段二：State Store（狀態管理）

### 概念說明

Dapr 的 State Store 將「狀態儲存」抽象化，你的程式碼只需呼叫 Dapr API，
底層可以是 Redis、PostgreSQL、MongoDB 等，切換時**不需修改程式碼**。

```
┌──────────┐      HTTP/gRPC      ┌──────────────┐      ┌───────────┐
│ 你的應用  │  ──────────────►   │ Dapr Sidecar │  ──► │  Redis    │
│ (Java)   │                     │ (daprd)      │      │  / PgSQL  │
└──────────┘                     └──────────────┘      └───────────┘
```

### 2.1 定義資料模型

```java
// src/main/java/com/example/daprdemo/model/Order.java
package com.example.daprdemo.model;

public record Order(String orderId, String product, Integer quantity, Double price) {}
```

> **為什麼用 record？** Java 16+ 引入的 record 是不可變資料類別，
> 自動產生 `equals()`、`hashCode()`、`toString()`，非常適合 DTO / 值物件。

### 2.2 建立 Repository

```java
// src/main/java/com/example/daprdemo/repository/OrderRepository.java
package com.example.daprdemo.repository;

import com.example.daprdemo.model.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, String> {}
```

### 2.3 設定 Dapr State Store

```java
// src/main/java/com/example/daprdemo/config/StateStoreConfig.java
package com.example.daprdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.statestore.config.DaprStateStoreProperties;
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
```

### 2.4 建立 REST Controller

```java
// src/main/java/com/example/daprdemo/controller/OrderController.java
package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import io.dapr.spring.data.DaprKeyValueTemplate;
import org.springframework.data.repository.config.EnableDaprRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
@EnableDaprRepositories
public class OrderController {

    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody Order order) {
        repository.save(order);
        return order;
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return repository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "訂單不存在: " + orderId));
    }

    @GetMapping
    public Iterable<Order> getAllOrders() {
        return repository.findAll();
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable String orderId) {
        repository.deleteById(orderId);
    }
}
```

### 2.5 設定 Dapr 屬性

```properties
# src/main/resources/application.properties（更新）
spring.application.name=dapr-demo
server.port=8080

# Dapr State Store 設定
dapr.statestore.name=kvstore
dapr.statestore.binding=kvbinding
```

### 2.6 測試：Controller 單元測試

```java
// src/test/java/com/example/daprdemo/controller/OrderControllerTest.java
package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_應回傳201與訂單資料() throws Exception {
        Order order = new Order("order-1", "筆電", 1, 35000.0);
        when(repository.save(any(Order.class))).thenReturn(order);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("order-1"))
            .andExpect(jsonPath("$.product").value("筆電"))
            .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void getOrder_存在時應回傳訂單() throws Exception {
        Order order = new Order("order-1", "滑鼠", 2, 500.0);
        when(repository.findById("order-1")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/order-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("order-1"))
            .andExpect(jsonPath("$.product").value("滑鼠"));
    }

    @Test
    void getOrder_不存在時應回傳404() throws Exception {
        when(repository.findById("not-exist")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/not-exist"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllOrders_應回傳訂單列表() throws Exception {
        List<Order> orders = List.of(
            new Order("order-1", "鍵盤", 1, 2000.0),
            new Order("order-2", "螢幕", 1, 12000.0)
        );
        when(repository.findAll()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
```

### 2.7 執行測試

```bash
mvn test
```

> **測試觀點**：
> - **單元測試**使用 `@MockitoBean` 模擬 Repository，不需要真正的 Dapr sidecar
> - 這確保 Controller 邏輯正確，不受外部基礎設施影響
> - 測試命名使用中文描述，讓測試報告即為規格文件

### 提交本階段

```bash
git add src/main/java/com/example/daprdemo/model/Order.java \
  src/main/java/com/example/daprdemo/repository/OrderRepository.java \
  src/main/java/com/example/daprdemo/config/StateStoreConfig.java \
  src/main/java/com/example/daprdemo/controller/OrderController.java \
  src/main/resources/application.properties \
  src/test/java/com/example/daprdemo/controller/OrderControllerTest.java

git commit -m "feat: 新增 Dapr State Store 狀態管理與訂單 CRUD 功能"
git push origin main
```

---

## 階段三：Pub/Sub（發布訂閱）

### 概念說明

Pub/Sub 讓服務之間透過**訊息**而非直接呼叫來通訊，實現鬆耦合：

```
┌──────────────┐   publish    ┌──────────────┐          ┌──────────────┐
│ OrderService │  ────────►  │ Dapr Sidecar │  ──────► │ Message      │
│ (Publisher)  │              │              │          │ Broker       │
└──────────────┘              └──────────────┘          │ (Redis)      │
                                                        └──────┬───────┘
                                                               │
                              ┌──────────────┐          ┌──────▼───────┐
                              │ Notification │  ◄────── │ Dapr Sidecar │
                              │ Service      │          │              │
                              │ (Subscriber) │          └──────────────┘
                              └──────────────┘
```

### 3.1 設定 Pub/Sub 屬性

```properties
# src/main/resources/application.properties（更新）
spring.application.name=dapr-demo
server.port=8080

# Dapr State Store 設定
dapr.statestore.name=kvstore
dapr.statestore.binding=kvbinding

# Dapr Pub/Sub 設定
dapr.pubsub.name=pubsub
```

### 3.2 建立事件模型

```java
// src/main/java/com/example/daprdemo/model/OrderEvent.java
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
```

### 3.3 建立 Publisher（發布者）

```java
// src/main/java/com/example/daprdemo/service/OrderEventPublisher.java
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
```

### 3.4 設定 Messaging Template

```java
// src/main/java/com/example/daprdemo/config/PubSubConfig.java
package com.example.daprdemo.config;

import com.example.daprdemo.model.OrderEvent;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.pubsub.config.DaprPubSubProperties;
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
        return new DaprMessagingTemplate<>(daprClient, properties.getName());
    }
}
```

### 3.5 建立 Subscriber（訂閱者）

```java
// src/main/java/com/example/daprdemo/controller/OrderEventSubscriber.java
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
```

### 3.6 更新 OrderController 加入事件發布

```java
// src/main/java/com/example/daprdemo/controller/OrderController.java（更新）
package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import com.example.daprdemo.service.OrderEventPublisher;
import io.dapr.spring.data.DaprKeyValueTemplate;
import org.springframework.data.repository.config.EnableDaprRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
@EnableDaprRepositories
public class OrderController {

    private final OrderRepository repository;
    private final OrderEventPublisher eventPublisher;

    public OrderController(OrderRepository repository,
                           OrderEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody Order order) {
        repository.save(order);
        eventPublisher.publishOrderCreated(order.orderId());
        return order;
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return repository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "訂單不存在: " + orderId));
    }

    @GetMapping
    public Iterable<Order> getAllOrders() {
        return repository.findAll();
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable String orderId) {
        repository.deleteById(orderId);
    }
}
```

### 3.7 測試：Publisher 單元測試

```java
// src/test/java/com/example/daprdemo/service/OrderEventPublisherTest.java
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
```

### 3.8 測試：更新後的 Controller 測試

```java
// src/test/java/com/example/daprdemo/controller/OrderControllerTest.java（更新）
package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import com.example.daprdemo.service.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository repository;

    @MockitoBean
    private OrderEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_應儲存訂單並發布事件() throws Exception {
        Order order = new Order("order-1", "筆電", 1, 35000.0);
        when(repository.save(any(Order.class))).thenReturn(order);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("order-1"));

        // 驗證事件確實被發布
        verify(eventPublisher).publishOrderCreated("order-1");
    }

    @Test
    void getOrder_存在時應回傳訂單() throws Exception {
        Order order = new Order("order-1", "滑鼠", 2, 500.0);
        when(repository.findById("order-1")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/order-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    void getOrder_不存在時應回傳404() throws Exception {
        when(repository.findById("not-exist")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/not-exist"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllOrders_應回傳訂單列表() throws Exception {
        List<Order> orders = List.of(
            new Order("order-1", "鍵盤", 1, 2000.0),
            new Order("order-2", "螢幕", 1, 12000.0)
        );
        when(repository.findAll()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
```

### 3.9 執行測試

```bash
mvn test
```

> **測試觀點**：
> - 使用 `verify()` 確認事件確實被發布，驗證**行為**而非僅驗證回傳值
> - `ArgumentCaptor` 可以捕捉實際傳入的參數，驗證事件的每個欄位是否正確
> - Publisher 和 Controller 測試是獨立的，各自驗證各自的職責（**單一職責原則**）

### 提交本階段

```bash
git add src/main/java/com/example/daprdemo/model/OrderEvent.java \
  src/main/java/com/example/daprdemo/service/OrderEventPublisher.java \
  src/main/java/com/example/daprdemo/config/PubSubConfig.java \
  src/main/java/com/example/daprdemo/controller/OrderEventSubscriber.java \
  src/main/java/com/example/daprdemo/controller/OrderController.java \
  src/main/resources/application.properties \
  src/test/java/com/example/daprdemo/service/OrderEventPublisherTest.java \
  src/test/java/com/example/daprdemo/controller/OrderControllerTest.java

git commit -m "feat: 新增 Pub/Sub 訊息發布訂閱機制與訂單事件通知"
git push origin main
```

---

## 階段四：整合測試（Testcontainers）

### 概念說明

單元測試用 Mock 隔離外部依賴，但**整合測試**需要真正的 Dapr sidecar 和基礎設施。
Testcontainers 可以在測試時自動啟動 Docker 容器，提供真實的運行環境。

```
測試金字塔：

    ╱  E2E  ╲          ← 少量，驗證完整流程
   ╱─────────╲
  ╱  整合測試  ╲        ← 本階段重點，驗證 Dapr 實際整合
 ╱─────────────╲
╱   單元測試    ╲       ← 大量，驗證個別元件邏輯（階段二、三）
╱───────────────╲
```

### 4.1 建立 Testcontainers 設定

```java
// src/test/java/com/example/daprdemo/config/DaprTestContainersConfig.java
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
```

### 4.2 建立整合測試

```java
// src/test/java/com/example/daprdemo/integration/OrderIntegrationTest.java
package com.example.daprdemo.integration;

import com.example.daprdemo.DaprDemoApplication;
import com.example.daprdemo.config.DaprTestContainersConfig;
import com.example.daprdemo.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = DaprDemoApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@Import(DaprTestContainersConfig.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        // 讓 Dapr sidecar 能回呼測試中的應用
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void 建立訂單_應透過Dapr儲存並回傳201() throws Exception {
        Order order = new Order("integration-1", "測試商品", 3, 999.0);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("integration-1"))
            .andExpect(jsonPath("$.product").value("測試商品"));
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void 查詢訂單_應從Dapr狀態儲存取回資料() throws Exception {
        mockMvc.perform(get("/orders/integration-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("integration-1"))
            .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void 刪除訂單_應從狀態儲存移除() throws Exception {
        mockMvc.perform(delete("/orders/integration-1"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/integration-1"))
            .andExpect(status().isNotFound());
    }
}
```

### 4.3 建立測試啟動器（用於開發模式）

```java
// src/test/java/com/example/daprdemo/TestDaprDemoApplication.java
package com.example.daprdemo;

import com.example.daprdemo.config.DaprTestContainersConfig;
import org.springframework.boot.SpringApplication;

public class TestDaprDemoApplication {
    public static void main(String[] args) {
        SpringApplication
            .from(DaprDemoApplication::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
    }
}
```

> 使用 `mvn spring-boot:test-run` 可以在開發模式下，自動啟動 Testcontainers 提供的 Dapr 環境。

### 4.4 執行整合測試

```bash
# 僅執行整合測試
mvn test -Dtest="*IntegrationTest"

# 執行所有測試（單元 + 整合）
mvn verify
```

> **測試觀點**：
> - 整合測試使用**真實的 Dapr sidecar** 和 **Redis**，不再是 Mock
> - `@TestMethodOrder` 確保測試按序執行，模擬真實使用情境
> - `exposeHostPorts(8080)` 讓容器中的 Dapr sidecar 能回呼宿主機上的應用
> - 整合測試較慢，應與單元測試分開執行，避免拖慢開發迭代速度

### 提交本階段

```bash
git add src/test/java/com/example/daprdemo/config/DaprTestContainersConfig.java \
  src/test/java/com/example/daprdemo/integration/OrderIntegrationTest.java \
  src/test/java/com/example/daprdemo/TestDaprDemoApplication.java

git commit -m "test: 新增 Testcontainers 整合測試驗證 Dapr 狀態管理與訊息發布"
git push origin main
```

---

## 階段五：使用 Dapr CLI 運行完整應用

### 5.1 建立 Dapr 元件設定檔

```yaml
# components/statestore.yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: kvstore
spec:
  type: state.redis
  version: v1
  metadata:
  - name: redisHost
    value: localhost:6379
  - name: redisPassword
    value: ""
```

```yaml
# components/pubsub.yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: pubsub
spec:
  type: pubsub.redis
  version: v1
  metadata:
  - name: redisHost
    value: localhost:6379
  - name: redisPassword
    value: ""
```

### 5.2 啟動應用

```bash
# 先編譯
mvn clean package -DskipTests

# 使用 Dapr CLI 啟動
dapr run \
  --app-id dapr-demo \
  --app-port 8080 \
  --resources-path ./components \
  -- java -jar target/dapr-demo-0.0.1-SNAPSHOT.jar
```

### 5.3 手動驗證（Smoke Test）

```bash
# 建立訂單
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId":"test-1","product":"手機","quantity":1,"price":25000}'

# 查詢訂單
curl http://localhost:8080/orders/test-1

# 查詢所有訂單
curl http://localhost:8080/orders

# 刪除訂單
curl -X DELETE http://localhost:8080/orders/test-1

# 確認已刪除
curl http://localhost:8080/orders/test-1
# 預期回傳 404

# 健康檢查
curl http://localhost:8080/health
```

### 5.4 驗證 Dapr Dashboard

```bash
dapr dashboard
# 開啟 http://localhost:8080 查看 Dapr 控制台
```

> **測試觀點**：
> - Smoke Test（煙霧測試）是最基本的端到端驗證，確認部署後的服務能正常回應
> - 使用 `curl` 手動測試時，檢查 HTTP 狀態碼與回應內容是否符合預期
> - 可將上述 `curl` 命令寫成 shell script 作為自動化 Smoke Test

### 提交本階段

```bash
git add components/statestore.yaml \
  components/pubsub.yaml

git commit -m "chore: 新增 Dapr 元件設定檔與完整運行指引"
git push origin main
```

---

## 專案結構總覽

```
dapr-demo/
├── pom.xml
├── components/
│   ├── statestore.yaml              # Dapr State Store 元件設定
│   └── pubsub.yaml                  # Dapr Pub/Sub 元件設定
├── src/
│   ├── main/
│   │   ├── java/com/example/daprdemo/
│   │   │   ├── DaprDemoApplication.java
│   │   │   ├── config/
│   │   │   │   ├── StateStoreConfig.java
│   │   │   │   └── PubSubConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── OrderEventSubscriber.java
│   │   │   ├── model/
│   │   │   │   ├── Order.java
│   │   │   │   └── OrderEvent.java
│   │   │   ├── repository/
│   │   │   │   └── OrderRepository.java
│   │   │   └── service/
│   │   │       └── OrderEventPublisher.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/daprdemo/
│           ├── DaprDemoApplicationTests.java
│           ├── TestDaprDemoApplication.java
│           ├── config/
│           │   └── DaprTestContainersConfig.java
│           ├── controller/
│           │   ├── HealthControllerTest.java
│           │   └── OrderControllerTest.java
│           ├── integration/
│           │   └── OrderIntegrationTest.java
│           └── service/
│               └── OrderEventPublisherTest.java
```

---

## 測試策略摘要

| 層級 | 檔案 | 驗證重點 | 是否需要 Dapr |
|------|------|---------|-------------|
| 單元測試 | `HealthControllerTest` | 健康檢查端點回傳格式 | 否 |
| 單元測試 | `OrderControllerTest` | CRUD 邏輯 + 事件發布行為 | 否（Mock） |
| 單元測試 | `OrderEventPublisherTest` | 事件內容正確性 | 否（Mock） |
| 整合測試 | `OrderIntegrationTest` | 端到端流程（真實 Dapr + Redis） | 是（Testcontainers） |
| 煙霧測試 | curl 命令 | 部署後基本功能 | 是（Dapr CLI） |

---

## 常見問題

### Q: `dapr init` 失敗怎麼辦？
確認 Docker 正在運行。Dapr 需要 Docker 來拉取 sidecar 映像和啟動 Redis。

### Q: 整合測試很慢怎麼辦？
Testcontainers 首次執行需要下載映像，後續會使用快取。也可以使用 `mvn test` 只跑單元測試，
在 CI 中再跑整合測試。

### Q: 如何切換 State Store 的後端（如改用 PostgreSQL）？
只需修改 `components/statestore.yaml` 的 `type` 和 `metadata`，Java 程式碼**完全不需要改動**。
這就是 Dapr 抽象化的價值。

### Q: Spring Boot 4 與 Dapr SDK 的相容性？
Dapr Spring Boot Starter 目前仍在 alpha 階段，baseline 為 Spring Boot 3.2+。
Spring Boot 4 基於 Spring Framework 7，有以下已知差異需注意：
- **Jackson 版本**：Spring Boot 4 使用 Jackson 3（`tools.jackson`），Dapr SDK 1.16 使用 Jackson 2（`com.fasterxml.jackson`），需手動建立 Jackson 2 `ObjectMapper` 給 Dapr 使用
- **模組化**：`@WebMvcTest` 和 `@AutoConfigureMockMvc` 搬到 `org.springframework.boot.webmvc.test.autoconfigure` package
- **測試依賴**：需額外引入 `spring-boot-starter-webmvc-test`

若遇到其他問題，請查閱 [Dapr Java SDK GitHub Issues](https://github.com/dapr/java-sdk/issues)。

---

## 參考資源

- [Dapr 官方文件](https://docs.dapr.io/)
- [Dapr Java SDK](https://github.com/dapr/java-sdk)
- [Dapr Spring Boot 整合指南](https://docs.dapr.io/developing-applications/sdks/java/spring-boot/)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [JDK 21 新功能](https://openjdk.org/projects/jdk/21/)
- [Testcontainers 官方文件](https://www.testcontainers.org/)
