package com.cs2hub.bootstrap;

import com.cs2hub.identity.dto.DevLoginRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Cs2SuperHubApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        PostgreSQLContainer<?> container = null;
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                container = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("cs2hub")
                        .withUsername("testuser")
                        .withPassword("testpass");
                container.start();
                log.info("Started Testcontainers PostgreSQL 16 container at {}", container.getJdbcUrl());
            }
        } catch (Exception ex) {
            log.warn("Testcontainers Docker environment not available: {}. Falling back to local PostgreSQL/H2.", ex.getMessage());
            container = null;
        }
        POSTGRES = container;
    }

    public static boolean isLocalPostgresAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDatabaseAvailable() {
        return (POSTGRES != null && POSTGRES.isRunning()) || isLocalPostgresAvailable();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        } else if (isLocalPostgresAvailable()) {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/cs2hub");
            registry.add("spring.datasource.username", () -> "postgres");
            registry.add("spring.datasource.password", () -> "9779");
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:cs2hub;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
        }
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.flyway.schemas", () -> "identity,loadouts,minigames,stratboard,matches");
        registry.add("app.auth.dev-mode-enabled", () -> "true");
        registry.add("app.matches.worker-secret", () -> "dev-worker-secret-key");
        registry.add("app.keepalive.enabled", () -> "false");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String obtainToken(String steamId, String username, String role) throws Exception {
        DevLoginRequestDto request = new DevLoginRequestDto(steamId, username, role);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    protected String obtainUserToken() throws Exception {
        return obtainToken("76561198000000001", "IntegrationUser", "USER");
    }

    protected String obtainAdminToken() throws Exception {
        return obtainToken("76561198000000002", "clutch_admin", "ADMIN");
    }
}
