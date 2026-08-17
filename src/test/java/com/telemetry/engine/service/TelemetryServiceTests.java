package com.telemetry.engine.service;

import com.telemetry.engine.config.TelemetryProperties;
import com.telemetry.engine.exception.TelemetryUnavailableException;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.pipeline.TelemetryPipeline;
import com.telemetry.engine.repository.TelemetryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryServiceTests {

    @Test
    void rejectsBackpressureAsServiceUnavailableAndRollsBackAdmission()
            throws Exception {
        TelemetryPipeline pipeline = mock(TelemetryPipeline.class);
        TelemetryRepository repository = mock(TelemetryRepository.class);
        TelemetryService service = new TelemetryService(
                pipeline,
                repository,
                new TelemetryProperties(),
                new SimpleMeterRegistry()
        );
        TelemetryPacket packet = new TelemetryPacket(
                "DRONE-BACKPRESSURE-001",
                100.0,
                20.0
        );

        when(repository.existsByPacketId(packet.getPacketId()))
                .thenReturn(false);
        when(repository.findTopByDeviceIdOrderBySequenceNumberDesc(
                packet.getDeviceId()
        )).thenReturn(java.util.Optional.empty());
        when(pipeline.submit(packet)).thenReturn(false, true);

        assertThatThrownBy(() -> service.processTelemetry(packet))
                .isInstanceOf(TelemetryUnavailableException.class)
                .hasMessageContaining("capacity");

        service.processTelemetry(packet);

        verify(pipeline, org.mockito.Mockito.times(2)).submit(packet);
    }
}
