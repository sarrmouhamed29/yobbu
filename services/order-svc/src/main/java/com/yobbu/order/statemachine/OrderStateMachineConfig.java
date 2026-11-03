package com.yobbu.order.statemachine;

import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.model.OrderStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory")
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatus, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderStatus.PENDING)
                .end(OrderStatus.DELIVERED)
                .end(OrderStatus.CANCELLED)
                .states(EnumSet.allOf(OrderStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(OrderStatus.PENDING).target(OrderStatus.CONFIRMED)
                    .event(OrderEvent.CONFIRM).and()
                .withExternal()
                    .source(OrderStatus.CONFIRMED).target(OrderStatus.READY)
                    .event(OrderEvent.MARK_READY).and()
                .withExternal()
                    .source(OrderStatus.READY).target(OrderStatus.PICKED_UP)
                    .event(OrderEvent.PICK_UP).and()
                .withExternal()
                    .source(OrderStatus.PICKED_UP).target(OrderStatus.DELIVERED)
                    .event(OrderEvent.DELIVER).and()
                .withExternal()
                    .source(OrderStatus.PENDING).target(OrderStatus.CANCELLED)
                    .event(OrderEvent.CANCEL).and()
                .withExternal()
                    .source(OrderStatus.CONFIRMED).target(OrderStatus.CANCELLED)
                    .event(OrderEvent.CANCEL);
    }
}
