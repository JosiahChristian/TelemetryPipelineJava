package com.telemetry.engine.api;

import com.telemetry.engine.model.TelemetryPacket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record TelemetryRequest(
        @NotBlank(message = "packetId is required")
        String packetId,
        @NotBlank(message = "deviceId is required")
        String deviceId,
        @NotNull(message = "sequenceNumber is required")
        @PositiveOrZero(message = "sequenceNumber must be zero or greater")
        Long sequenceNumber,
        @PositiveOrZero(message = "altitude must be zero or greater")
        double altitude,
        @PositiveOrZero(message = "velocity must be zero or greater")
        double velocity,
        @NotNull(message = "timestamp is required")
        Instant timestamp
) {

    public TelemetryPacket toEntity() {
        TelemetryPacket packet = new TelemetryPacket();
        packet.setPacketId(packetId);
        packet.setDeviceId(deviceId);
        packet.setSequenceNumber(sequenceNumber);
        packet.setAltitude(altitude);
        packet.setVelocity(velocity);
        packet.setTimestamp(timestamp);
        return packet;
    }
}
