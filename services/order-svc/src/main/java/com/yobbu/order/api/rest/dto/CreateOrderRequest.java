package com.yobbu.order.api.rest.dto;

import com.yobbu.order.domain.model.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull OrderType type,
        @NotNull UUID customerId,
        UUID merchantId,
        @NotBlank String pickupAddress,
        @NotBlank String deliveryAddress,
        String deliveryNotes,
        @NotBlank String currency,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotBlank String name,
            @Min(1) int quantity,
            @Min(0) int unitPriceCents
    ) {}
}
