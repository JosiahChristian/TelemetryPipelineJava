package com.telemetry.engine.pipeline;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;


@Component
public class TelemetryPipeline {


    private final BlockingQueue<String> telemetryBuffer;

    private volatile boolean running;


    public TelemetryPipeline() {

        this.telemetryBuffer =
                new LinkedBlockingQueue<>(100);

        this.running = true;
    }


    public void startPipeline() {


        Thread telemetryProducer = new Thread(() -> {

            int packetId = 1;

            try {

                while (running && packetId <= 5) {


                    String packet =
                            "PACKET_ID_"
                            + packetId
                            + " [ALTITUDE="
                            + (20 + packetId * 5)
                            + "m]";


                    telemetryBuffer.put(packet);


                    System.out.println(
                            "[PRODUCER] Ingested raw network stream: "
                            + packet
                    );


                    packetId++;

                    Thread.sleep(100);

                }


            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }


        });



        Thread pipelineConsumer = new Thread(() -> {


            try {


                while (running || !telemetryBuffer.isEmpty()) {


                    String packet =
                            telemetryBuffer.poll(
                                    150,
                                    TimeUnit.MILLISECONDS
                            );


                    if (packet != null) {


                        System.out.println(
                                "[CONSUMER] Thread Safe worker processed & stored: "
                                + packet
                                + " -> [DB_SUCCESS]"
                        );

                    }


                }


            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }


        });



        telemetryProducer.start();

        pipelineConsumer.start();



        try {


            telemetryProducer.join();


            running = false;


            pipelineConsumer.join();


        } catch (InterruptedException e) {


            Thread.currentThread().interrupt();

        }


    }

}