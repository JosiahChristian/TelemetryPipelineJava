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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
