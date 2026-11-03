package com.yobbu.order.application;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.model.OrderStatus;
import com.yobbu.order.domain.port.in.CreateOrderUseCase;
import com.yobbu.order.domain.port.in.GetOrderUseCase;
import com.yobbu.order.domain.port.in.TransitionOrderUseCase;
import com.yobbu.order.domain.port.outgoing.OrderRepository;
import com.yobbu.order.statemachine.OrderStateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class OrderService implements CreateOrderUseCase, GetOrderUseCase, TransitionOrderUseCase {

    private final OrderRepository repository;
    private final OrderStateMachineService stateMachine;

    public OrderService(OrderRepository repository, OrderStateMachineService stateMachine) {
        this.repository = repository;
        this.stateMachine = stateMachine;
    }

    @Override
    public Order create(CreateOrderCommand command) {
        int total = command.items().stream()
                .mapToInt(i -> i.quantity() * i.unitPriceCents())
                .sum();
        Order order = new Order(
                command.type(), command.customerId(),
                command.pickupAddress(), command.deliveryAddress(),
                command.deliveryNotes(), total, command.currency()
        );
        command.items().forEach(i -> order.addItem(i.name(), i.quantity(), i.unitPriceCents()));
        return repository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getByCustomer(UUID customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Override
    public Order transition(UUID orderId, OrderEvent event) {
        Order order = getById(orderId);
        OrderStatus next = stateMachine.transition(order.getStatus(), event);
        order.applyStatus(next);
        return repository.save(order);
    }
}
