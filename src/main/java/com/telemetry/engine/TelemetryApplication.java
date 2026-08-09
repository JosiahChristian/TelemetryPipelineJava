package com.telemetry.engine;

import com.telemetry.engine.service.TelemetryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TelemetryApplication implements CommandLineRunner {


    private final TelemetryService telemetryService;


    public TelemetryApplication(TelemetryService telemetryService) {

        this.telemetryService = telemetryService;

    }


    public static void main(String[] args) {

        SpringApplication.run(
                TelemetryApplication.class,
                args
        );

    }


    @Override
    public void run(String... args) {

        telemetryService.startTelemetryProcessing();

    }

}