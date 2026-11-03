package com.yobbu.order.statemachine;

import com.yobbu.order.domain.model.DeliveryEvent;
import com.yobbu.order.domain.model.DeliveryStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "deliveryStateMachineFactory")
public class DeliveryStateMachineConfig extends StateMachineConfigurerAdapter<DeliveryStatus, DeliveryEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<DeliveryStatus, DeliveryEvent> states) throws Exception {
        states.withStates()
                .initial(DeliveryStatus.PENDING_ASSIGNMENT)
                .end(DeliveryStatus.DELIVERED)
                .end(DeliveryStatus.FAILED)
                .states(EnumSet.allOf(DeliveryStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<DeliveryStatus, DeliveryEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(DeliveryStatus.PENDING_ASSIGNMENT).target(DeliveryStatus.ASSIGNED)
                    .event(DeliveryEvent.ASSIGN).and()
                .withExternal()
                    .source(DeliveryStatus.ASSIGNED).target(DeliveryStatus.HEADING_TO_MERCHANT)
                    .event(DeliveryEvent.HEAD_TO_MERCHANT).and()
                .withExternal()
                    .source(DeliveryStatus.HEADING_TO_MERCHANT).target(DeliveryStatus.AT_MERCHANT)
                    .event(DeliveryEvent.ARRIVE_AT_MERCHANT).and()
                .withExternal()
                    .source(DeliveryStatus.AT_MERCHANT).target(DeliveryStatus.HEADING_TO_CUSTOMER)
                    .event(DeliveryEvent.HEAD_TO_CUSTOMER).and()
                .withExternal()
                    .source(DeliveryStatus.HEADING_TO_CUSTOMER).target(DeliveryStatus.DELIVERED)
                    .event(DeliveryEvent.COMPLETE).and()
                .withExternal()
                    .source(DeliveryStatus.ASSIGNED).target(DeliveryStatus.FAILED)
                    .event(DeliveryEvent.FAIL).and()
                .withExternal()
                    .source(DeliveryStatus.HEADING_TO_MERCHANT).target(DeliveryStatus.FAILED)
                    .event(DeliveryEvent.FAIL).and()
                .withExternal()
                    .source(DeliveryStatus.AT_MERCHANT).target(DeliveryStatus.FAILED)
                    .event(DeliveryEvent.FAIL).and()
                .withExternal()
                    .source(DeliveryStatus.HEADING_TO_CUSTOMER).target(DeliveryStatus.FAILED)
                    .event(DeliveryEvent.FAIL);
    }
}
