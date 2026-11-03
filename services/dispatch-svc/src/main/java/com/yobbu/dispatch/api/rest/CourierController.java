package com.yobbu.dispatch.api.rest;

import com.yobbu.dispatch.domain.model.Courier;
import com.yobbu.dispatch.domain.model.CourierStatus;
import com.yobbu.dispatch.infrastructure.persistence.JpaCourierRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Couriers", description = "Courier management — stub, full dispatch logic in phase 1.3")
@RestController
@RequestMapping("/api/v1/couriers")
public class CourierController {

    private final JpaCourierRepository repository;

    public CourierController(JpaCourierRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("service", "dispatch-svc", "status", "ok"));
    }

    @GetMapping
    public ResponseEntity<List<Courier>> list(@RequestParam(required = false) CourierStatus status) {
        if (status != null) {
            return ResponseEntity.ok(repository.findByStatus(status));
        }
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Courier> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
