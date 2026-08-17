package com.telemetry.engine.pipeline;

import com.telemetry.engine.config.TelemetryProperties;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.repository.TelemetryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryPipeline.class);
    private final BlockingQueue<TelemetryPacket> telemetryBuffer;
    private final TelemetryRepository telemetryRepository;
    private final TelemetryProperties properties;
    private final Counter persistedPackets;
    private final Counter persistenceFailures;

    private volatile boolean running;
    private Thread consumerThread;

    public TelemetryPipeline(
            TelemetryRepository telemetryRepository,
            TelemetryProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.telemetryRepository = telemetryRepository;
        this.properties = properties;
        this.telemetryBuffer = new LinkedBlockingQueue<>(
                properties.getQueueCapacity()
        );
        this.persistedPackets = meterRegistry.counter(
                "telemetry.persistence.total",
                "outcome",
                "success"
        );
        this.persistenceFailures = meterRegistry.counter(
                "telemetry.persistence.total",
                "outcome",
                "failure"
        );
        Gauge.builder(
                        "telemetry.queue.depth",
                        telemetryBuffer,
                        BlockingQueue::size
                )
                .description("Current number of packets awaiting persistence")
                .register(meterRegistry);
    }

    public boolean submit(TelemetryPacket packet) throws InterruptedException {
        return telemetryBuffer.offer(
                packet,
                properties.getSubmitTimeout().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @PostConstruct
    public void startConsumer() {
        if (running) {
            return;
        }

        running = true;

        consumerThread = new Thread(() -> {

            try {

                while (running || !telemetryBuffer.isEmpty()) {

                    TelemetryPacket packet =
                            telemetryBuffer.poll(
                                    properties.getPollInterval().toMillis(),
                                    TimeUnit.MILLISECONDS
                            );

                    if (packet != null) {

                        persist(packet);
                    }
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        }, "telemetry-persistence-worker");

        consumerThread.start();
    }

    private void persist(TelemetryPacket packet) {
        try {
            telemetryRepository.save(packet);
            persistedPackets.increment();
            LOGGER.debug(
                    "Persisted telemetry from device {}",
                    packet.getDeviceId()
            );
        } catch (RuntimeException exception) {
            persistenceFailures.increment();
            LOGGER.error(
                    "Unable to persist telemetry packet {} from device {}",
                    packet.getPacketId(),
                    packet.getDeviceId(),
                    exception
            );
        }
    }

    @PreDestroy
    public void stopConsumer() {
        running = false;

        if (consumerThread == null) {
            return;
        }

        try {
            consumerThread.join(properties.getShutdownTimeout().toMillis());

            if (consumerThread.isAlive()) {
                consumerThread.interrupt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getQueueDepth() {
        return telemetryBuffer.size();
    }

    public int getCapacity() {
        return properties.getQueueCapacity();
    }

    public boolean isRunning() {
        return running && consumerThread != null && consumerThread.isAlive();
    }
}
