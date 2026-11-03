package com.yobbu.order.statemachine;

import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class OrderStateMachineServiceTest {

    @Autowired
    OrderStateMachineService service;

    @Test
    void confirm_from_pending_transitions_to_confirmed() {
        OrderStatus next = service.transition(OrderStatus.PENDING, OrderEvent.CONFIRM);
        assertThat(next).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void mark_ready_from_confirmed_transitions_to_ready() {
        assertThat(service.transition(OrderStatus.CONFIRMED, OrderEvent.MARK_READY))
                .isEqualTo(OrderStatus.READY);
    }

    @Test
    void pick_up_from_ready_transitions_to_picked_up() {
        assertThat(service.transition(OrderStatus.READY, OrderEvent.PICK_UP))
                .isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    void deliver_from_picked_up_transitions_to_delivered() {
        assertThat(service.transition(OrderStatus.PICKED_UP, OrderEvent.DELIVER))
                .isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancel_from_pending_transitions_to_cancelled() {
        assertThat(service.transition(OrderStatus.PENDING, OrderEvent.CANCEL))
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_from_confirmed_transitions_to_cancelled() {
        assertThat(service.transition(OrderStatus.CONFIRMED, OrderEvent.CANCEL))
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void invalid_transition_throws() {
        assertThatThrownBy(() -> service.transition(OrderStatus.DELIVERED, OrderEvent.CONFIRM))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transition not allowed");
    }

    @Test
    void can_transition_returns_false_for_invalid() {
        assertThat(service.canTransition(OrderStatus.DELIVERED, OrderEvent.CONFIRM)).isFalse();
    }
}
