package com.yobbu.dispatch.api.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/v1/offers/ping")
    public ResponseEntity<Map<String, String>> pingOffers() {
        return ResponseEntity.ok(Map.of("service", "dispatch-svc", "status", "ok"));
    }

    @GetMapping("/api/v1/couriers/ping")
    public ResponseEntity<Map<String, String>> pingCouriers() {
        return ResponseEntity.ok(Map.of("service", "dispatch-svc", "status", "ok"));
    }
}
