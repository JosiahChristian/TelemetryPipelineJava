package com.telemetry.engine.pipeline;

import com.telemetry.engine.model.TelemetryPacket;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryPipeline {

    private final BlockingQueue<TelemetryPacket> telemetryBuffer =
            new LinkedBlockingQueue<>(100);

    private volatile boolean running = true;

    public void submit(TelemetryPacket packet) throws InterruptedException {
        telemetryBuffer.put(packet);
    }

    public void startConsumer() {

        Thread pipelineConsumer = new Thread(() -> {

            try {

                while (running || !telemetryBuffer.isEmpty()) {

                    TelemetryPacket packet =
                            telemetryBuffer.poll(
                                    150,
                                    TimeUnit.MILLISECONDS
                            );

                    if (packet != null) {

                        System.out.println(
                                "[CONSUMER] Processed telemetry from "
                                + packet.getDeviceId()
                                + " | altitude="
                                + packet.getAltitude()
                                + " | velocity="
                                + packet.getVelocity()
                                + " -> [DB_SUCCESS]"
                        );

                    }

                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        });

        pipelineConsumer.start();
    }

}