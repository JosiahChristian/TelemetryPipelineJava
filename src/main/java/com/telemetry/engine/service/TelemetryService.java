package com.telemetry.engine.service;

import com.telemetry.engine.pipeline.TelemetryPipeline;
import org.springframework.stereotype.Service;


@Service
public class TelemetryService {


    private final TelemetryPipeline pipeline;


    public TelemetryService(TelemetryPipeline pipeline) {

        this.pipeline = pipeline;

    }


    public void startTelemetryProcessing() {

        pipeline.startPipeline();

    }

}