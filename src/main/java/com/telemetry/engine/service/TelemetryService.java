package com.telemetry.engine.service;

import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.exception.DuplicateTelemetryException;
import com.telemetry.engine.exception.OutOfOrderTelemetryException;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import com.telemetry.engine.repository.TelemetryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TelemetryService {

    private final TelemetryPipeline pipeline;
    private final TelemetryRepository telemetryRepository;
    private final Set<String> acceptedPacketIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Long> highestSequenceByDevice =
            new ConcurrentHashMap<>();

    public TelemetryService(
            TelemetryPipeline pipeline,
            TelemetryRepository telemetryRepository
    ) {
        this.pipeline = pipeline;
        this.telemetryRepository = telemetryRepository;
    }

    public void processTelemetry(TelemetryPacket packet) {
        if (telemetryRepository.existsByPacketId(packet.getPacketId())
                || !acceptedPacketIds.add(packet.getPacketId())) {
            throw new DuplicateTelemetryException(packet.getPacketId());
        }

        AtomicReference<Long> previousSequence = new AtomicReference<>();

        try {
            highestSequenceByDevice.compute(packet.getDeviceId(), (deviceId, highest) -> {
                Long effectiveHighest = highest;

                if (effectiveHighest == null) {
                    effectiveHighest = telemetryRepository
                            .findTopByDeviceIdOrderBySequenceNumberDesc(deviceId)
                            .map(TelemetryPacket::getSequenceNumber)
                            .orElse(null);
                }

                previousSequence.set(effectiveHighest);

                if (effectiveHighest != null
                        && packet.getSequenceNumber() <= effectiveHighest) {
                    throw new OutOfOrderTelemetryException(
                            deviceId,
                            packet.getSequenceNumber(),
                            effectiveHighest
                    );
                }

                return packet.getSequenceNumber();
            });
        } catch (OutOfOrderTelemetryException exception) {
            acceptedPacketIds.remove(packet.getPacketId());
            throw exception;
        }

        try {
            if (!pipeline.submit(packet)) {
                rollbackAdmission(packet, previousSequence.get());
                throw new IllegalStateException(
                        "Telemetry queue is at capacity; retry the request"
                );
            }
        } catch (InterruptedException e) {
            rollbackAdmission(packet, previousSequence.get());
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Telemetry processing was interrupted",
                    e
            );
        }
    }

    private void rollbackAdmission(
            TelemetryPacket packet,
            Long previousSequence
    ) {
        acceptedPacketIds.remove(packet.getPacketId());

        if (previousSequence == null) {
            highestSequenceByDevice.remove(
                    packet.getDeviceId(),
                    packet.getSequenceNumber()
            );
        } else {
            highestSequenceByDevice.replace(
                    packet.getDeviceId(),
                    packet.getSequenceNumber(),
                    previousSequence
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
