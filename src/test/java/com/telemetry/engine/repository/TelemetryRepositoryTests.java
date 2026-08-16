package com.telemetry.engine.repository;

import com.telemetry.engine.model.TelemetryPacket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TelemetryRepositoryTests {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    void savesAndRetrievesTelemetryPacket() {

        TelemetryPacket packet =
                new TelemetryPacket(
                        "DRONE-DB-TEST-001",
                        175.0,
                        48.5
                );

        TelemetryPacket savedPacket =
                telemetryRepository.save(packet);

        TelemetryPacket retrievedPacket =
                telemetryRepository
                        .findById(savedPacket.getId())
                        .orElseThrow();

        assertThat(retrievedPacket.getId()).isNotNull();
        assertThat(retrievedPacket.getDeviceId())
                .isEqualTo("DRONE-DB-TEST-001");
        assertThat(retrievedPacket.getPacketId()).isNotBlank();
        assertThat(retrievedPacket.getSequenceNumber()).isZero();
        assertThat(retrievedPacket.getAltitude())
                .isEqualTo(175.0);
        assertThat(retrievedPacket.getVelocity())
                .isEqualTo(48.5);
        assertThat(retrievedPacket.getTimestamp())
                .isNotNull();
    }

    @Test
    void findsHighestPersistedSequenceForDevice() {
        TelemetryPacket first = new TelemetryPacket(
                "DRONE-SEQUENCE-001",
                175.0,
                48.5
        );
        first.setSequenceNumber(4L);

        TelemetryPacket second = new TelemetryPacket(
                "DRONE-SEQUENCE-001",
                180.0,
                50.0
        );
        second.setSequenceNumber(7L);

        telemetryRepository.save(first);
        telemetryRepository.save(second);

        TelemetryPacket highest = telemetryRepository
                .findTopByDeviceIdOrderBySequenceNumberDesc(
                        "DRONE-SEQUENCE-001"
                )
                .orElseThrow();

        assertThat(highest.getSequenceNumber()).isEqualTo(7L);
    }
}
