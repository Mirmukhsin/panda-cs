package com.cs2hub.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.keepalive.enabled", havingValue = "true", matchIfMissing = true)
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final RestClient restClient;
    private final String keepAliveUrl;

    public KeepAliveScheduler(
            @Value("${app.keepalive.url:https://panda-cs.onrender.com/api/v1/health}") String keepAliveUrl
    ) {
        this(keepAliveUrl, createDefaultRestClient());
    }

    public KeepAliveScheduler(String keepAliveUrl, RestClient restClient) {
        this.keepAliveUrl = keepAliveUrl;
        this.restClient = restClient;
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Scheduled(fixedRateString = "${app.keepalive.interval-ms:600000}", initialDelay = 60000)
    public void pingBackend() {
        try {
            log.info("Sending keep-alive ping to {}", keepAliveUrl);
            var response = restClient.get()
                    .uri(keepAliveUrl)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Keep-alive ping to {} succeeded with status: {}", keepAliveUrl, response.getStatusCode());
        } catch (Exception ex) {
            log.warn("Keep-alive ping to {} failed gracefully: {}", keepAliveUrl, ex.getMessage());
        }
    }

    public String getKeepAliveUrl() {
        return keepAliveUrl;
    }
}
