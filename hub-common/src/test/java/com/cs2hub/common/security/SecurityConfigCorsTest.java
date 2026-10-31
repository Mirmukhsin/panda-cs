package com.cs2hub.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    @DisplayName("Should permit production Vercel origin and preview wildcard patterns")
    void shouldPermitProductionAndPreviewVercelOrigins() {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        ObjectMapper objectMapper = new ObjectMapper();
        String allowedOrigins = "http://localhost:3000,http://localhost:5173,https://all-in-one-cs2.vercel.app";

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, objectMapper, allowedOrigins);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/loadouts/generate");

        // Test production Vercel domain
        CorsConfiguration configProd = source.getCorsConfiguration(request);
        assertThat(configProd).isNotNull();
        assertThat(configProd.checkOrigin("https://all-in-one-cs2.vercel.app")).isEqualTo("https://all-in-one-cs2.vercel.app");

        // Test Vercel preview branch deployment
        assertThat(configProd.checkOrigin("https://panda-cs-preview-123.vercel.app")).isEqualTo("https://panda-cs-preview-123.vercel.app");

        // Test Localhost origins
        assertThat(configProd.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configProd.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");

        // Test disallowed domain
        assertThat(configProd.checkOrigin("https://malicious-site.com")).isNull();

        // Test allowed methods and credentials
        assertThat(configProd.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configProd.getAllowCredentials()).isTrue();
        assertThat(configProd.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Guest-Session-Id", "*");
    }
}
