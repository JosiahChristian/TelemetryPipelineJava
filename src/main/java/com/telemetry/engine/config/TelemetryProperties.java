package com.telemetry.engine.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "telemetry")
@Validated
public class TelemetryProperties {

    @Min(1)
    private int queueCapacity = 100;

    @NotNull
    private Duration submitTimeout = Duration.ofMillis(250);

    @NotNull
    private Duration pollInterval = Duration.ofMillis(150);

    @NotNull
    private Duration shutdownTimeout = Duration.ofSeconds(1);

    @NotNull
    private Duration maxPacketAge = Duration.ofMinutes(5);

    @NotNull
    private Duration futureClockSkew = Duration.ofSeconds(30);

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public Duration getSubmitTimeout() {
        return submitTimeout;
    }

    public void setSubmitTimeout(Duration submitTimeout) {
        this.submitTimeout = submitTimeout;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public Duration getMaxPacketAge() {
        return maxPacketAge;
    }

    public void setMaxPacketAge(Duration maxPacketAge) {
        this.maxPacketAge = maxPacketAge;
    }

    public Duration getFutureClockSkew() {
        return futureClockSkew;
    }

    public void setFutureClockSkew(Duration futureClockSkew) {
        this.futureClockSkew = futureClockSkew;
    }
}
