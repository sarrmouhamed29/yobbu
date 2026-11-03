package com.yobbu.order.api.rest;

import com.yobbu.order.api.rest.dto.CreateOrderRequest;
import com.yobbu.order.api.rest.dto.OrderResponse;
import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.port.in.CreateOrderUseCase;
import com.yobbu.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.yobbu.order.domain.port.in.GetOrderUseCase;
import com.yobbu.order.domain.port.in.TransitionOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase create;
    private final GetOrderUseCase get;
    private final TransitionOrderUseCase transition;

    public OrderController(CreateOrderUseCase create, GetOrderUseCase get, TransitionOrderUseCase transition) {
        this.create = create;
        this.get = get;
        this.transition = transition;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            UriComponentsBuilder uriBuilder) {

        var command = new CreateOrderCommand(
                req.type(), req.customerId(), req.merchantId(),
                req.pickupAddress(), req.deliveryAddress(), req.deliveryNotes(),
                0, req.currency(),
                req.items().stream()
                        .map(i -> new CreateOrderCommand.ItemCommand(i.name(), i.quantity(), i.unitPriceCents()))
                        .toList()
        );
        var order = create.create(command);
        var uri = uriBuilder.path("/api/v1/orders/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(uri).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(get.getById(id)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam UUID customerId) {
        var orders = get.getByCustomer(customerId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/transitions")
    public ResponseEntity<OrderResponse> applyTransition(
            @PathVariable UUID id,
            @RequestParam OrderEvent event) {
        return ResponseEntity.ok(OrderResponse.from(transition.transition(id, event)));
    }

    @GetMapping("/ping")
    public ResponseEntity<java.util.Map<String, String>> ping() {
        return ResponseEntity.ok(java.util.Map.of("service", "order-svc", "status", "ok"));
    }
}
