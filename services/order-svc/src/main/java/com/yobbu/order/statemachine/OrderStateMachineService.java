package com.yobbu.order.statemachine;

import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.model.OrderStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Validates order state transitions without persisting machine state.
 * The authoritative state lives in the orders table; the machine is ephemeral.
 */
@Service
public class OrderStateMachineService {

    private final StateMachineFactory<OrderStatus, OrderEvent> factory;

    public OrderStateMachineService(
            @org.springframework.beans.factory.annotation.Qualifier("orderStateMachineFactory")
            StateMachineFactory<OrderStatus, OrderEvent> factory) {
        this.factory = factory;
    }

    public boolean canTransition(OrderStatus current, OrderEvent event) {
        StateMachine<OrderStatus, OrderEvent> sm = buildMachine(UUID.randomUUID().toString(), current);
        sm.sendEvent(
                org.springframework.messaging.support.MessageBuilder.withPayload(event).build()
        );
        return sm.getState().getId() != current;
    }

    public OrderStatus transition(OrderStatus current, OrderEvent event) {
        StateMachine<OrderStatus, OrderEvent> sm = buildMachine(UUID.randomUUID().toString(), current);
        sm.sendEvent(
                org.springframework.messaging.support.MessageBuilder.withPayload(event).build()
        );
        OrderStatus next = sm.getState().getId();
        if (next == current) {
            throw new IllegalStateException(
                    "Transition not allowed: " + current + " --[" + event + "]--> ?");
        }
        return next;
    }

    private StateMachine<OrderStatus, OrderEvent> buildMachine(String machineId, OrderStatus initialState) {
        StateMachine<OrderStatus, OrderEvent> sm = factory.getStateMachine(machineId);
        sm.stopReactively().block();
        sm.getStateMachineAccessor()
                .doWithAllRegions(accessor ->
                        accessor.resetStateMachineReactively(
                                new DefaultStateMachineContext<>(initialState, null, null, null)
                        ).block()
                );
        sm.startReactively().block();
        return sm;
    }
}
