package com.yobbu.order.domain.port.outgoing;

import com.yobbu.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findByCustomerId(UUID customerId);
}
