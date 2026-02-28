package com.example.daprdemo.controller;

import com.example.daprdemo.model.Order;
import com.example.daprdemo.repository.OrderRepository;
import com.example.daprdemo.service.OrderEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
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
