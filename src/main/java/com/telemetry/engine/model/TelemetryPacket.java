package com.telemetry.engine.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public class TelemetryPacket {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @PositiveOrZero(message = "altitude must be zero or greater")
    private double altitude;

    @PositiveOrZero(message = "velocity must be zero or greater")
    private double velocity;

    private Instant timestamp;

    public TelemetryPacket() {
        this.timestamp = Instant.now();
    }

    public TelemetryPacket(
            String deviceId,
            double altitude,
            double velocity
    ) {
        this.deviceId = deviceId;
        this.altitude = altitude;
        this.velocity = velocity;
        this.timestamp = Instant.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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