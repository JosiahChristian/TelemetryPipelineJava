package com.telemetry.engine.service;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import com.telemetry.engine.repository.TelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public void processTelemetry(TelemetryPacket packet) {
        try {
            if (!pipeline.submit(packet)) {
                throw new IllegalStateException(
                        "Telemetry queue is at capacity; retry the request"
                );
            }
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

    public Optional<TelemetryPacket> getTelemetryById(Long id) {
        return telemetryRepository.findById(id);
    }

    public TelemetryPipelineStatus getPipelineStatus() {
        return new TelemetryPipelineStatus(
                pipeline.isRunning(),
                pipeline.getQueueDepth(),
                pipeline.getCapacity()
        );
    }

    public record TelemetryPipelineStatus(
            boolean running,
            int queueDepth,
            int capacity
    ) {
    }
}
