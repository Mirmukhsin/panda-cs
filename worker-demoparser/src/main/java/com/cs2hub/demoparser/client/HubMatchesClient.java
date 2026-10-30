package com.cs2hub.demoparser.client;

import com.cs2hub.demoparser.dto.ApiResponseWrapper;
import com.cs2hub.demoparser.dto.ClaimMatchJobDto;
import com.cs2hub.demoparser.dto.FailMatchJobRequestDto;
import com.cs2hub.demoparser.dto.ParsedMatchPayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class HubMatchesClient {

    private final RestClient restClient;
    private final String workerSecret;

    public HubMatchesClient(
            @Value("${worker.hub-base-url:http://localhost:8080}") String hubBaseUrl,
            @Value("${worker.secret:dev-worker-secret-key}") String workerSecret,
            @Value("${worker.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${worker.read-timeout-ms:30000}") int readTimeoutMs
    ) {
        this.workerSecret = workerSecret;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(hubBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<ClaimMatchJobDto> claimNextJob() {
        try {
            ApiResponseWrapper<ClaimMatchJobDto> response = restClient.post()
                    .uri("/api/v1/internal/matches/claim-next")
                    .header("X-Worker-Secret", workerSecret)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseWrapper<ClaimMatchJobDto>>() {});

            if (response != null && response.data() != null) {
                return Optional.of(response.data());
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to claim next match job from hub: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void completeJob(UUID matchId, ParsedMatchPayloadDto payload) {
        restClient.post()
                .uri("/api/v1/internal/matches/{matchId}/complete", matchId)
                .header("X-Worker-Secret", workerSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.info("Reported match parsing completion for match: {}", matchId);
    }

    public void failJob(UUID matchId, String reason) {
        try {
            restClient.post()
                    .uri("/api/v1/internal/matches/{matchId}/fail", matchId)
                    .header("X-Worker-Secret", workerSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FailMatchJobRequestDto(reason))
                    .retrieve()
                    .toBodilessEntity();
            log.warn("Reported match failure to hub for match {}: {}", matchId, reason);
        } catch (Exception ex) {
            log.error("Failed to report match failure to hub for match {}: {}", matchId, ex.getMessage(), ex);
        }
    }
}
