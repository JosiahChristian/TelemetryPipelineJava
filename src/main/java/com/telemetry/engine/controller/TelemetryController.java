package com.telemetry.engine.controller;

import com.telemetry.engine.api.TelemetryAdmissionResponse;
import com.telemetry.engine.api.TelemetryRequest;
import com.telemetry.engine.api.TelemetryResponse;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.service.TelemetryService;
import com.telemetry.engine.service.TelemetryService.TelemetryPipelineStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TelemetryAdmissionResponse> receiveTelemetry(
            @Valid @RequestBody TelemetryRequest request
    ) {
        TelemetryPacket packet = request.toEntity();
        telemetryService.processTelemetry(packet);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new TelemetryAdmissionResponse(
                        packet.getPacketId(),
                        packet.getDeviceId(),
                        packet.getSequenceNumber(),
                        "queued"
                )
        );
    }

    @GetMapping
    @Operation(summary = "Retrieve all persisted telemetry packets")
    public ResponseEntity<Page<TelemetryResponse>> getTelemetry(
            @RequestParam(required = false) String deviceId,
            @PageableDefault(
                    size = 50,
                    sort = "timestamp",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(telemetryService.searchTelemetry(deviceId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve one persisted telemetry packet")
    public ResponseEntity<TelemetryResponse> getTelemetryById(
            @PathVariable Long id
    ) {
        return telemetryService
                .getTelemetryById(id)
                .map(TelemetryResponse::from)
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
