package com.telemetry.engine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise Telemetry Event Ingestion Pipeline
 * Demonstrates proficiency in Java Multi-threading, Concurrent Data Structures,
 * and robust Object-Oriented Design patterns for corporate software systems.
 */
public class Main {

    // Concurrent thread-safe buffer queue to ingest high-frequency data packets
    private static final BlockingQueue<String> telemetryBuffer = new LinkedBlockingQueue<>(100);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("     ENTERPRISE JAVA HIGH-FREQUENCY TELEMETRY PIPELINE        ");
        System.out.println("===============================================================");
        System.out.println("Initializing ingestion workers and concurrent buffer matrices...\n");

        // Thread 1: The Producer (Simulating incoming high-frequency drone sensor packets)
        Thread telemetryProducer = new Thread(() -> {
            int packetId = 1;
            try {
                while (running && packetId <= 5) {
                    String packet = "PACKET_ID_" + packetId + " [ALTITUDE=" + (20 + packetId * 5) + "m]";
                    telemetryBuffer.put(packet);
                    System.out.println("[PRODUCER] Ingested raw network stream: " + packet);
                    packetId++;
                    Thread.sleep(100); // 100ms stream delay
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Thread 2: The Consumer (Simulating a backend processing database worker engine)
        Thread pipelineConsumer = new Thread(() -> {
            try {
                while (running || !telemetryBuffer.isEmpty()) {
                    String packet = telemetryBuffer.poll(150, TimeUnit.MILLISECONDS);
                    if (packet != null) {
                        System.out.println("[CONSUMER] Thread Safe worker processed & stored: " + packet + " -> [DB_SUCCESS]");
                    } else if (!running) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Boot up concurrent thread workers asynchronously
        telemetryProducer.start();
        pipelineConsumer.start();

        try {
            telemetryProducer.join(); // Wait for data ingestion to finish
            running = false;          // Gracefully signal consumer to shut down
            pipelineConsumer.join();
        } catch (InterruptedException e) {
            System.err.println("Main pipeline execution interrupted.");
        }

        System.out.println("\n---------------------------------------------------------------");
        System.out.println("JAVA TELEMETRY INGESTION ENGINE LIFECYCLE COMPLETE.");
        System.out.println("All thread workers joined cleanly. System shutdown safe.");
        System.out.println("===============================================================");
    }
}
