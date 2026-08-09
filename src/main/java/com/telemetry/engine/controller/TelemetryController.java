package com.telemetry.engine.controller;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public ResponseEntity<String> receiveTelemetry(
            @Valid @RequestBody TelemetryPacket packet
    ) {
        telemetryService.processTelemetry(packet);

        return ResponseEntity.ok(
                "Telemetry queued for processing from device: "
                        + packet.getDeviceId()
        );
    }

    @GetMapping
    public ResponseEntity<List<TelemetryPacket>> getAllTelemetry() {
        return ResponseEntity.ok(
                telemetryService.getAllTelemetry()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelemetryPacket> getTelemetryById(
            @PathVariable Long id
    ) {
        return telemetryService
                .getTelemetryById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Telemetry API is operational");
    }
}