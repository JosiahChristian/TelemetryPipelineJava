package com.telemetry.engine.service;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {

    private final TelemetryPipeline pipeline;

    public TelemetryService(TelemetryPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void startTelemetryProcessing() {
        pipeline.startConsumer();
    }

    public void processTelemetry(TelemetryPacket packet) {
        try {
            pipeline.submit(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Telemetry processing was interrupted",
                    e
            );
        }
    }
}