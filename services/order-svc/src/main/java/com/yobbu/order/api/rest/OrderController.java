package com.yobbu.order.api.rest;

import com.yobbu.order.api.rest.dto.CreateOrderRequest;
import com.yobbu.order.api.rest.dto.OrderResponse;
import com.yobbu.order.domain.model.OrderEvent;
import com.yobbu.order.domain.port.in.CreateOrderUseCase;
import com.yobbu.order.domain.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.yobbu.order.domain.port.in.GetOrderUseCase;
import com.yobbu.order.domain.port.in.TransitionOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders", description = "Order lifecycle management")
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

    @Operation(summary = "Create a new order")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
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

    @Operation(summary = "Get an order by ID")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(get.getById(id)));
    }

    @Operation(summary = "List orders for a customer")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam UUID customerId) {
        var orders = get.getByCustomer(customerId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Apply a state transition to an order",
               description = "Allowed transitions: PENDING→CONFIRM, CONFIRMED→MARK_READY, READY→PICK_UP, PICKED_UP→DELIVER, PENDING/CONFIRMED→CANCEL")
    @ApiResponse(responseCode = "200", description = "Transition applied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "422", description = "Transition not allowed from current state")
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
