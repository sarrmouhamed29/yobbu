package com.yobbu.order.domain.port.in;

import com.yobbu.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

public interface GetOrderUseCase {
    Order getById(UUID id);
    List<Order> getByCustomer(UUID customerId);
}
