package com.yobbu.order.domain.port.in;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.model.OrderEvent;

import java.util.List;
import java.util.UUID;

public interface CreateOrderUseCase {

    record CreateOrderCommand(
            com.yobbu.order.domain.model.OrderType type,
            UUID customerId,
            UUID merchantId,
            String pickupAddress,
            String deliveryAddress,
            String deliveryNotes,
            int amountCents,
            String currency,
            List<ItemCommand> items
    ) {
        public record ItemCommand(String name, int quantity, int unitPriceCents) {}
    }

    Order create(CreateOrderCommand command);
}
