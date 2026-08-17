package com.telemetry.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.engine.model.TelemetryPacket;
import com.telemetry.engine.repository.TelemetryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    void healthEndpointReturnsPipelineStatus() throws Exception {
        mockMvc.perform(get("/api/telemetry/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.queueDepth").isNumber())
                .andExpect(jsonPath("$.capacity").value(100));
    }

    @Test
    void openApiDocumentDescribesTelemetryEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Telemetry Pipeline API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$['paths']['/api/telemetry']").exists())
                .andExpect(jsonPath("$['paths']['/api/telemetry/{id}']").exists());
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void actuatorHealthReportsApplicationUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void prometheusEndpointExposesTelemetryMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("telemetry_queue_depth")
                ))
                .andExpect(content().string(
                        containsString("telemetry_admission_total")
                ));
    }

    @Test
    void telemetryPostAcceptsPacket() throws Exception {
        TelemetryPacket packet =
                new TelemetryPacket(
                        "DRONE-TEST-001",
                        120.5,
                        35.2
                );

        mockMvc.perform(
                        post("/api/telemetry")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(packet))
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "Telemetry queued for processing from device: DRONE-TEST-001"
                        )
                );
    }

    @Test
    void telemetryPostRejectsInvalidPacket() throws Exception {
        TelemetryPacket packet =
                new TelemetryPacket(
                        "",
                        -10.0,
                        -5.0
                );

        mockMvc.perform(
                        post("/api/telemetry")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(packet))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/telemetry"));
    }

    @Test
    void telemetryPostRejectsDuplicatePacketId() throws Exception {
        TelemetryPacket packet = new TelemetryPacket(
                "DRONE-DUPLICATE-001",
                100.0,
                20.0
        );
        packet.setSequenceNumber(1L);

        String request = objectMapper.writeValueAsString(packet);

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate telemetry"));
    }

    @Test
    void telemetryPostRejectsOutOfOrderSequence() throws Exception {
        TelemetryPacket newest = new TelemetryPacket(
                "DRONE-ORDER-001",
                100.0,
                20.0
        );
        newest.setSequenceNumber(10L);

        TelemetryPacket stale = new TelemetryPacket(
                "DRONE-ORDER-001",
                99.0,
                19.0
        );
        stale.setSequenceNumber(9L);

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stale)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Out-of-order telemetry"));
    }

    @Test
    void telemetryPostRejectsStaleTimestamp() throws Exception {
        TelemetryPacket stale = new TelemetryPacket(
                "DRONE-STALE-001",
                100.0,
                20.0
        );
        stale.setTimestamp(Instant.now().minusSeconds(600));

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stale)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error")
                        .value("Invalid telemetry timestamp"));
    }

    @Test
    void telemetryPostRejectsFutureTimestampBeyondClockSkew() throws Exception {
        TelemetryPacket future = new TelemetryPacket(
                "DRONE-FUTURE-001",
                100.0,
                20.0
        );
        future.setTimestamp(Instant.now().plusSeconds(120));

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(future)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error")
                        .value("Invalid telemetry timestamp"));
    }

    @Test
    void telemetryGetByIdReturnsPersistedPacket() throws Exception {
        TelemetryPacket packet =
                new TelemetryPacket(
                        "DRONE-ID-001",
                        200.0,
                        55.0
                );

        TelemetryPacket savedPacket =
                telemetryRepository.save(packet);

        mockMvc.perform(
                        get("/api/telemetry/" + savedPacket.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPacket.getId()))
                .andExpect(jsonPath("$.deviceId").value("DRONE-ID-001"))
                .andExpect(jsonPath("$.altitude").value(200.0))
                .andExpect(jsonPath("$.velocity").value(55.0));
    }

    @Test
    void telemetryGetByIdReturnsNotFoundForMissingPacket() throws Exception {
        mockMvc.perform(get("/api/telemetry/999999"))
                .andExpect(status().isNotFound());
    }
}
