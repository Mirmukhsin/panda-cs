package com.cs2hub.demoparser.service;

import com.cs2hub.demoparser.client.HubMatchesClient;
import com.cs2hub.demoparser.dto.ClaimMatchJobDto;
import com.cs2hub.demoparser.dto.ParsedMatchPayloadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoWorkerPollerTest {

    @Mock
    private HubMatchesClient hubMatchesClient;

    @Mock
    private DemoFileDownloader demoFileDownloader;

    @Mock
    private Cs2DemoParserEngine parserEngine;

    private DemoWorkerPoller poller;

    @BeforeEach
    void setUp() {
        poller = new DemoWorkerPoller(hubMatchesClient, demoFileDownloader, parserEngine);
        ReflectionTestUtils.setField(poller, "pollingEnabled", true);
    }

    @Test
    @DisplayName("Should successfully process match job, complete on hub, and delete temp demo file")
    void shouldSuccessfullyProcessJobAndCleanUpTempFile() throws IOException {
        UUID matchId = UUID.randomUUID();
        ClaimMatchJobDto job = new ClaimMatchJobDto(matchId, "CSGO-TEST-CODE", "http://demos.cs2hub.com/sample.dem", "share-slug-1");
        Path tempFile = Files.createTempFile("test-worker-", ".dem");
        ParsedMatchPayloadDto payload = new ParsedMatchPayloadDto("de_mirage", 13, 8, 2100, Collections.emptyList());

        when(hubMatchesClient.claimNextJob()).thenReturn(Optional.of(job));
        when(demoFileDownloader.downloadDemo(job.demoUrl())).thenReturn(tempFile);
        when(parserEngine.parse(matchId, job.shareCode(), tempFile)).thenReturn(payload);

        poller.pollAndProcess();

        verify(hubMatchesClient).completeJob(matchId, payload);
        verify(hubMatchesClient, never()).failJob(any(), any());
        assertThat(Files.exists(tempFile)).isFalse(); // Ephemeral cleanup verification
    }

    @Test
    @DisplayName("Should report failure to hub and delete temp demo file when parsing throws an exception")
    void shouldReportFailureToHubAndCleanUpTempFileWhenParsingThrows() throws IOException {
        UUID matchId = UUID.randomUUID();
        ClaimMatchJobDto job = new ClaimMatchJobDto(matchId, "CSGO-FAIL-CODE", "http://demos.cs2hub.com/corrupt.dem", "share-slug-2");
        Path tempFile = Files.createTempFile("test-worker-fail-", ".dem");

        when(hubMatchesClient.claimNextJob()).thenReturn(Optional.of(job));
        when(demoFileDownloader.downloadDemo(job.demoUrl())).thenReturn(tempFile);
        when(parserEngine.parse(matchId, job.shareCode(), tempFile)).thenThrow(new IllegalArgumentException("Invalid demo header"));

        poller.pollAndProcess();

        verify(hubMatchesClient).failJob(eq(matchId), contains("Invalid demo header"));
        verify(hubMatchesClient, never()).completeJob(any(), any());
        assertThat(Files.exists(tempFile)).isFalse(); // Ephemeral cleanup verification even on failure
    }

    @Test
    @DisplayName("Should do nothing when queue is empty")
    void shouldDoNothingWhenQueueIsEmpty() throws IOException {
        when(hubMatchesClient.claimNextJob()).thenReturn(Optional.empty());

        poller.pollAndProcess();

        verify(demoFileDownloader, never()).downloadDemo(any());
        verify(parserEngine, never()).parse(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip polling cycle when polling is disabled")
    void shouldSkipPollingWhenDisabled() {
        poller.setPollingEnabled(false);

        poller.pollAndProcess();

        verify(hubMatchesClient, never()).claimNextJob();
    }
}
