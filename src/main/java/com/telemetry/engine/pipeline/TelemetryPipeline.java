package com.telemetry.engine.pipeline;

import com.telemetry.engine.config.TelemetryProperties;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.repository.TelemetryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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

    private volatile boolean running;
    private Thread consumerThread;

    public TelemetryPipeline(
            TelemetryRepository telemetryRepository,
            TelemetryProperties properties
    ) {
        this.telemetryRepository = telemetryRepository;
        this.properties = properties;
        this.telemetryBuffer = new LinkedBlockingQueue<>(
                properties.getQueueCapacity()
        );
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

                        telemetryRepository.save(packet);

                        LOGGER.debug(
                                "Persisted telemetry from device {}",
                                packet.getDeviceId()
                        );
                    }
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        }, "telemetry-persistence-worker");

        consumerThread.start();
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
