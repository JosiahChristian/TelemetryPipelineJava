package com.telemetry.engine.service;

import com.telemetry.engine.config.TelemetryProperties;
import com.telemetry.engine.exception.DuplicateTelemetryException;
import com.telemetry.engine.exception.OutOfOrderTelemetryException;
import com.telemetry.engine.exception.TelemetryTimestampException;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import com.telemetry.engine.repository.TelemetryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final TelemetryProperties properties;
    private final Counter acceptedPackets;
    private final Counter duplicatePackets;
    private final Counter outOfOrderPackets;
    private final Counter invalidTimestampPackets;
    private final Counter backpressureRejections;
    private final Set<String> acceptedPacketIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Long> highestSequenceByDevice =
            new ConcurrentHashMap<>();

    public TelemetryService(
            TelemetryPipeline pipeline,
            TelemetryRepository telemetryRepository,
            TelemetryProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.pipeline = pipeline;
        this.telemetryRepository = telemetryRepository;
        this.properties = properties;
        this.acceptedPackets = admissionCounter(meterRegistry, "accepted");
        this.duplicatePackets = admissionCounter(meterRegistry, "duplicate");
        this.outOfOrderPackets = admissionCounter(meterRegistry, "out_of_order");
        this.invalidTimestampPackets = admissionCounter(meterRegistry, "invalid_timestamp");
        this.backpressureRejections = admissionCounter(meterRegistry, "backpressure");
    }

    public void processTelemetry(TelemetryPacket packet) {
        validateTimestamp(packet);

        if (telemetryRepository.existsByPacketId(packet.getPacketId())
                || !acceptedPacketIds.add(packet.getPacketId())) {
            duplicatePackets.increment();
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
            outOfOrderPackets.increment();
            throw exception;
        }

        try {
            if (!pipeline.submit(packet)) {
                rollbackAdmission(packet, previousSequence.get());
                backpressureRejections.increment();
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

        acceptedPackets.increment();
    }

    private Counter admissionCounter(MeterRegistry meterRegistry, String outcome) {
        return Counter.builder("telemetry.admission.total")
                .description("Telemetry packet admission outcomes")
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private void validateTimestamp(TelemetryPacket packet) {
        Instant now = Instant.now();
        Instant oldestAccepted = now.minus(properties.getMaxPacketAge());
        Instant newestAccepted = now.plus(properties.getFutureClockSkew());

        if (packet.getTimestamp().isBefore(oldestAccepted)) {
            invalidTimestampPackets.increment();
            throw new TelemetryTimestampException(
                    "Telemetry timestamp exceeds the maximum packet age"
            );
        }

        if (packet.getTimestamp().isAfter(newestAccepted)) {
            invalidTimestampPackets.increment();
            throw new TelemetryTimestampException(
                    "Telemetry timestamp exceeds the permitted future clock skew"
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
