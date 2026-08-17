package com.telemetry.engine.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

@Entity
public class TelemetryPacket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotBlank(message = "packetId is required")
    @Column(nullable = false, unique = true, updatable = false)
    private String packetId;

    @NotNull(message = "sequenceNumber is required")
    @PositiveOrZero(message = "sequenceNumber must be zero or greater")
    private Long sequenceNumber;

    private double altitude;

    private double velocity;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    public TelemetryPacket() {
    }

    public TelemetryPacket(
            String deviceId,
            double altitude,
            double velocity
    ) {
        this.deviceId = deviceId;
        this.packetId = java.util.UUID.randomUUID().toString();
        this.sequenceNumber = 0L;
        this.altitude = altitude;
        this.velocity = velocity;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
