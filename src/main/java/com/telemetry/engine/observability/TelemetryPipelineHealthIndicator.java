package com.telemetry.engine.observability;

import com.telemetry.engine.pipeline.TelemetryPipeline;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("telemetryPipeline")
public class TelemetryPipelineHealthIndicator implements HealthIndicator {

    private final TelemetryPipeline pipeline;

    public TelemetryPipelineHealthIndicator(TelemetryPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public Health health() {
        Health.Builder status = pipeline.isRunning()
                ? Health.up()
                : Health.down();

        return status
                .withDetail("queueDepth", pipeline.getQueueDepth())
                .withDetail("queueCapacity", pipeline.getCapacity())
                .build();
    }
}
