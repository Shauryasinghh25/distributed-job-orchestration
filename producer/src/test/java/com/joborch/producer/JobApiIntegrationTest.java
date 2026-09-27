package com.joborch.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joborch.common.domain.JobStatus;
import com.joborch.common.domain.JobType;
import com.joborch.producer.dto.CreateJobRequest;
import com.joborch.producer.dto.JobResponse;
import com.joborch.producer.repository.JobRepository;
import com.joborch.producer.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the Job REST API — Phase 18.
 * Uses real PostgreSQL + RabbitMQ via Testcontainers.
 * Tests the full HTTP → Service → DB pipeline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class JobApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JobRepository jobRepository;
    @Autowired OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/jobs creates job, persists to DB, writes outbox event")
    void createJob_success() throws Exception {
        CreateJobRequest req = CreateJobRequest.builder()
                .name("Test Email Job")
                .jobType(JobType.EMAIL)
                .payload(Map.of("recipient", "test@example.com", "subject", "Hello"))
                .idempotencyKey("test-key-" + UUID.randomUUID())
                .build();

        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.jobType").value("EMAIL"))
                .andReturn();

        JobResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), JobResponse.class);

        // Verify DB persistence
        assertThat(jobRepository.findById(response.getId())).isPresent();
        assertThat(jobRepository.findById(response.getId()).get().getStatus())
                .isEqualTo(JobStatus.QUEUED);

        // Verify outbox event was written
        assertThat(outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/jobs with same idempotencyKey returns existing job")
    void createJob_idempotent() throws Exception {
        String key = "idempotent-key-" + UUID.randomUUID();
        CreateJobRequest req = CreateJobRequest.builder()
                .name("Idempotent Job")
                .jobType(JobType.NOTIFICATION)
                .idempotencyKey(key)
                .build();

        // First call
        MvcResult first = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        // Second call — same idempotency key
        MvcResult second = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        JobResponse r1 = objectMapper.readValue(first.getResponse().getContentAsString(), JobResponse.class);
        JobResponse r2 = objectMapper.readValue(second.getResponse().getContentAsString(), JobResponse.class);

        // Same job returned both times
        assertThat(r1.getId()).isEqualTo(r2.getId());
        // Only ONE job in DB
        assertThat(jobRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/jobs/{id} returns 404 for unknown ID")
    void getJob_notFound() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/jobs validates required fields")
    void createJob_validation_failure() throws Exception {
        String badJson = """
                { "priority": "NORMAL" }
                """;
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/jobs/{id}/status returns current status")
    void getJobStatus() throws Exception {
        String key = "status-test-" + UUID.randomUUID();
        CreateJobRequest req = CreateJobRequest.builder()
                .name("Status Test Job")
                .jobType(JobType.DATA_PROCESSING)
                .idempotencyKey(key)
                .build();

        MvcResult created = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        JobResponse job = objectMapper.readValue(created.getResponse().getContentAsString(), JobResponse.class);

        mockMvc.perform(get("/api/jobs/{id}/status", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }
}
