package com.telemetry.engine;

import com.telemetry.engine.service.TelemetryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TelemetryStartupRunner implements CommandLineRunner {

    private final TelemetryService telemetryService;

    public TelemetryStartupRunner(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    public void run(String... args) {
        telemetryService.startTelemetryProcessing();
    }
}