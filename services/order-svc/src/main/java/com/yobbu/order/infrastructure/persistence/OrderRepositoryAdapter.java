package com.yobbu.order.infrastructure.persistence;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.port.out.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpa;

    public OrderRepositoryAdapter(JpaOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
