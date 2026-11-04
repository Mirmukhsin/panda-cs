package com.cs2hub.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeepAliveSchedulerTest {

    @Test
    @DisplayName("pingBackend should make GET request to keepAliveUrl and handle success")
    void shouldSendKeepAlivePingSuccessfully() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        KeepAliveScheduler scheduler = new KeepAliveScheduler("https://panda-cs.onrender.com/api/v1/health", restClient);
        assertThat(scheduler.getKeepAliveUrl()).isEqualTo("https://panda-cs.onrender.com/api/v1/health");

        assertThatCode(scheduler::pingBackend).doesNotThrowAnyException();
        verify(uriSpec).uri("https://panda-cs.onrender.com/api/v1/health");
    }

    @Test
    @DisplayName("pingBackend should gracefully catch and log any connection exception without throwing")
    void shouldCatchExceptionGracefully() {
        RestClient restClient = mock(RestClient.class);
        when(restClient.get()).thenThrow(new RestClientException("Connection timed out"));

        KeepAliveScheduler scheduler = new KeepAliveScheduler("https://panda-cs.onrender.com/api/v1/health", restClient);

        assertThatCode(scheduler::pingBackend).doesNotThrowAnyException();
    }
}
