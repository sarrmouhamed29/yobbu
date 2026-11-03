package com.yobbu.order.domain.port.in;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.model.OrderEvent;

import java.util.UUID;

public interface TransitionOrderUseCase {
    Order transition(UUID orderId, OrderEvent event);
}
