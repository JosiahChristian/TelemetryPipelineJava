package com.telemetry.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.engine.model.TelemetryPacket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReturnsOperationalMessage() throws Exception {
        mockMvc.perform(get("/api/telemetry/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Telemetry API is operational"));
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
}