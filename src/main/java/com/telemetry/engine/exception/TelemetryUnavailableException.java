package com.telemetry.engine.exception;

public class TelemetryUnavailableException extends RuntimeException {

    public TelemetryUnavailableException(String message) {
        super(message);
    }

    public TelemetryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
