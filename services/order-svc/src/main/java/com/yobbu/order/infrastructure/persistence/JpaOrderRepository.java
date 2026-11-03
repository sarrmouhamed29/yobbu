package com.yobbu.order.infrastructure.persistence;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Order> findByStatus(OrderStatus status);
}
