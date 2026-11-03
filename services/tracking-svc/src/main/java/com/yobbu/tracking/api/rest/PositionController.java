package com.yobbu.tracking.api.rest;

import com.yobbu.tracking.domain.model.CourierPosition;
import com.yobbu.tracking.infrastructure.persistence.JpaPositionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final JpaPositionRepository repository;

    public PositionController(JpaPositionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("service", "tracking-svc", "status", "ok"));
    }

    @GetMapping("/couriers/{courierId}/latest")
    public ResponseEntity<CourierPosition> latest(@PathVariable UUID courierId) {
        return repository.findLatestByCourierId(courierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
