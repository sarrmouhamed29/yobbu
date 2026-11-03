package com.yobbu.order.application;

import com.yobbu.order.domain.model.*;
import com.yobbu.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.yobbu.order.domain.port.outgoing.OrderRepository;
import com.yobbu.order.statemachine.OrderStateMachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository repository;

    @Mock
    OrderStateMachineService stateMachine;

    @InjectMocks
    OrderService service;

    private CreateOrderCommand command;

    @BeforeEach
    void setUp() {
        command = new CreateOrderCommand(
                OrderType.MEAL, UUID.randomUUID(), null,
                "Marché Sandaga", "Plateau, Dakar",
                null, 0, "XOF",
                List.of(new CreateOrderCommand.ItemCommand("Thieboudienne", 2, 3000))
        );
    }

    @Test
    void create_computes_total_and_saves() {
        Order saved = new Order(OrderType.MEAL, command.customerId(),
                command.pickupAddress(), command.deliveryAddress(),
                null, 6000, "XOF");
        when(repository.save(any())).thenReturn(saved);

        Order result = service.create(command);

        assertThat(result.getAmountCents()).isEqualTo(6000);
        verify(repository, times(1)).save(any());
    }

    @Test
    void getById_throws_when_not_found() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void transition_delegates_to_state_machine_and_saves() {
        Order order = new Order(OrderType.MEAL, UUID.randomUUID(),
                "a", "b", null, 1000, "XOF");
        when(repository.findById(any())).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.PENDING, OrderEvent.CONFIRM))
                .thenReturn(OrderStatus.CONFIRMED);
        when(repository.save(order)).thenReturn(order);

        service.transition(UUID.randomUUID(), OrderEvent.CONFIRM);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository).save(order);
    }
}
