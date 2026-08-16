package com.telemetry.engine.exception;

public class DuplicateTelemetryException extends RuntimeException {

    public DuplicateTelemetryException(String packetId) {
        super("Telemetry packet already accepted: " + packetId);
    }
}
