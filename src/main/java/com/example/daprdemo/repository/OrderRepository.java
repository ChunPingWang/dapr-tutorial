package com.example.daprdemo.repository;

import com.example.daprdemo.model.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, String> {}
