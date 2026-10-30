package com.cs2hub.demoparser.service;

import com.cs2hub.demoparser.client.HubMatchesClient;
import com.cs2hub.demoparser.dto.ClaimMatchJobDto;
import com.cs2hub.demoparser.dto.ParsedMatchPayloadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoWorkerPoller {

    private final HubMatchesClient hubMatchesClient;
    private final DemoFileDownloader demoFileDownloader;
    private final Cs2DemoParserEngine parserEngine;

    @Value("${worker.polling-enabled:true}")
    private boolean pollingEnabled;

    @Scheduled(fixedDelayString = "${worker.poll-interval-ms:5000}")
    public void pollAndProcess() {
        if (!pollingEnabled) {
            log.trace("Demo worker polling is disabled; skipping cycle");
            return;
        }

        Optional<ClaimMatchJobDto> jobOpt = hubMatchesClient.claimNextJob();
        if (jobOpt.isEmpty()) {
            return;
        }

        ClaimMatchJobDto job = jobOpt.get();
        log.info("Claimed match job for processing: matchId={}, shareCode={}", job.matchId(), job.shareCode());

        Path tempDemoFile = null;
        try {
            if (job.demoUrl() != null && !job.demoUrl().isBlank()) {
                tempDemoFile = demoFileDownloader.downloadDemo(job.demoUrl());
            }

            ParsedMatchPayloadDto payload = parserEngine.parse(job.matchId(), job.shareCode(), tempDemoFile);
            hubMatchesClient.completeJob(job.matchId(), payload);
            log.info("Successfully completed demo parsing for match: {}", job.matchId());
        } catch (Exception ex) {
            log.error("Failed to process demo for match {}: {}", job.matchId(), ex.getMessage(), ex);
            String reason = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            hubMatchesClient.failJob(job.matchId(), reason);
        } finally {
            if (tempDemoFile != null) {
                try {
                    Files.deleteIfExists(tempDemoFile);
                    log.debug("Deleted ephemeral demo file: {}", tempDemoFile);
                } catch (IOException ioEx) {
                    log.warn("Failed to delete ephemeral demo file: {}", tempDemoFile, ioEx);
                }
            }
        }
    }

    public void setPollingEnabled(boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public boolean isPollingEnabled() {
        return this.pollingEnabled;
    }
}
