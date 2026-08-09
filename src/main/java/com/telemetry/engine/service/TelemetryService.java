package com.telemetry.engine.service;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import com.telemetry.engine.repository.TelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelemetryService {

    private final TelemetryPipeline pipeline;
    private final TelemetryRepository telemetryRepository;

    public TelemetryService(
            TelemetryPipeline pipeline,
            TelemetryRepository telemetryRepository
    ) {
        this.pipeline = pipeline;
        this.telemetryRepository = telemetryRepository;
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

    public List<TelemetryPacket> getAllTelemetry() {
        return telemetryRepository.findAll();
    }
}