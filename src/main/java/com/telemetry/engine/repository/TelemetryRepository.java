package com.telemetry.engine.repository;

import com.telemetry.engine.model.TelemetryPacket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRepository
        extends JpaRepository<TelemetryPacket, Long> {
}