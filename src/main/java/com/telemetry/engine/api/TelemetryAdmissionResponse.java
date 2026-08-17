package com.telemetry.engine.api;

public record TelemetryAdmissionResponse(
        String packetId,
        String deviceId,
        Long sequenceNumber,
        String status
) {
}
