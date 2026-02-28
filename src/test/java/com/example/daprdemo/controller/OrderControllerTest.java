package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import com.example.daprdemo.service.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
