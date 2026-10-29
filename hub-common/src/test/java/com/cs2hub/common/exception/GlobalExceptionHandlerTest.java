package com.cs2hub.common.exception;

import com.cs2hub.common.dto.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should wrap ResourceNotFoundException in 404 ErrorResponseDto")
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found with id 123");
        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("User not found with id 123");
    }

    @Test
    @DisplayName("Should wrap BadRequestException in 400 ErrorResponseDto")
    void shouldHandleBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid SteamID format");
        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("Invalid SteamID format");
    }

    @Test
    @DisplayName("Should wrap UnauthorizedException in 401 ErrorResponseDto")
    void shouldHandleUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Token has expired");
        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().message()).isEqualTo("Token has expired");
    }

    @Test
    @DisplayName("Should wrap generic Exception in 500 ErrorResponseDto")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected database timeout");
        ResponseEntity<ErrorResponseDto> response = exceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected database timeout");
    }
}
