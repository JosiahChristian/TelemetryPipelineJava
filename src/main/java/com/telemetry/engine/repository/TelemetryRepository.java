package com.telemetry.engine.repository;

import com.telemetry.engine.model.TelemetryPacket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelemetryRepository
        extends JpaRepository<TelemetryPacket, Long> {

    boolean existsByPacketId(String packetId);

    Optional<TelemetryPacket> findTopByDeviceIdOrderBySequenceNumberDesc(
            String deviceId
    );
}
