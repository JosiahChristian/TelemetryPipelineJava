package com.telemetry.engine.api;

import com.telemetry.engine.model.TelemetryPacket;

import java.time.Instant;

public record TelemetryResponse(
        Long id,
        String packetId,
        String deviceId,
        Long sequenceNumber,
        double altitude,
        double velocity,
        Instant timestamp
) {

    public static TelemetryResponse from(TelemetryPacket packet) {
        return new TelemetryResponse(
                packet.getId(),
                packet.getPacketId(),
                packet.getDeviceId(),
                packet.getSequenceNumber(),
                packet.getAltitude(),
                packet.getVelocity(),
                packet.getTimestamp()
        );
    }
}
