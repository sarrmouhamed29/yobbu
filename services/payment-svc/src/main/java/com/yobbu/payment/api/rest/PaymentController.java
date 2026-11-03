package com.yobbu.payment.api.rest;

import com.yobbu.payment.application.SimulatedPaymentService;
import com.yobbu.payment.domain.model.Payment;
import com.yobbu.payment.infrastructure.persistence.JpaPaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Payments", description = "Simulated mobile-money payments — no real money is processed")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final SimulatedPaymentService service;
    private final JpaPaymentRepository repository;

    public PaymentController(SimulatedPaymentService service, JpaPaymentRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("service", "payment-svc", "status", "ok"));
    }

    @PostMapping
    public ResponseEntity<Payment> initiate(@RequestBody InitiateRequest req) {
        return ResponseEntity.ok(service.initiate(req.orderId(), req.amountCents(), req.currency(), req.idempotencyKey()));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Payment> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Payment> getByOrder(@PathVariable UUID orderId) {
        return repository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record InitiateRequest(
            @NotNull UUID orderId,
            @Min(1) int amountCents,
            @NotBlank String currency,
            @NotBlank String idempotencyKey
    ) {}
}
