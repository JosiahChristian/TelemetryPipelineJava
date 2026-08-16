package com.telemetry.engine.controller;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.service.TelemetryService;
import com.telemetry.engine.service.TelemetryService.TelemetryPipelineStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry", description = "Telemetry ingestion and retrieval operations")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    @Operation(summary = "Submit a telemetry packet for asynchronous persistence")
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
    @Operation(summary = "Retrieve all persisted telemetry packets")
    public ResponseEntity<List<TelemetryPacket>> getAllTelemetry() {
        return ResponseEntity.ok(
                telemetryService.getAllTelemetry()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve one persisted telemetry packet")
    public ResponseEntity<TelemetryPacket> getTelemetryById(
            @PathVariable Long id
    ) {
        return telemetryService
                .getTelemetryById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    @Operation(summary = "Inspect the telemetry pipeline worker and queue")
    public ResponseEntity<TelemetryPipelineStatus> healthCheck() {
        TelemetryPipelineStatus status = telemetryService.getPipelineStatus();

        if (!status.running()) {
            return ResponseEntity.status(503).body(status);
        }

        return ResponseEntity.ok(status);
    }
}
