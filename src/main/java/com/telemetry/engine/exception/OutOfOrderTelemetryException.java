package com.telemetry.engine.exception;

public class OutOfOrderTelemetryException extends RuntimeException {

    public OutOfOrderTelemetryException(
            String deviceId,
            long receivedSequence,
            long highestSequence
    ) {
        super(
                "Out-of-order telemetry for device " + deviceId
                        + ": received sequence " + receivedSequence
                        + " after " + highestSequence
        );
    }
}
