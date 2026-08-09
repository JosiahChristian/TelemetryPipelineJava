package com.telemetry.engine.model;

import java.time.Instant;

public class TelemetryPacket {

    private String deviceId;
    private double altitude;
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