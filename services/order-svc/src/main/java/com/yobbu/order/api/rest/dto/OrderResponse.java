package com.yobbu.order.api.rest.dto;

import com.yobbu.order.domain.model.Order;
import com.yobbu.order.domain.model.OrderStatus;
import com.yobbu.order.domain.model.OrderType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderType type,
        OrderStatus status,
        UUID customerId,
        UUID merchantId,
        String pickupAddress,
        String deliveryAddress,
        String deliveryNotes,
        int amountCents,
        String currency,
        List<ItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record ItemResponse(UUID id, String name, int quantity, int unitPriceCents) {}

    public static OrderResponse from(Order order) {
        List<ItemResponse> items = order.getItems().stream()
                .map(i -> new ItemResponse(i.getId(), i.getName(), i.getQuantity(), i.getUnitPriceCents()))
                .toList();
        return new OrderResponse(
                order.getId(), order.getType(), order.getStatus(),
                order.getCustomerId(), order.getMerchantId(),
                order.getPickupAddress(), order.getDeliveryAddress(), order.getDeliveryNotes(),
                order.getAmountCents(), order.getCurrency(),
                items, order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}
