package com.cs2hub.common.security;

import com.cs2hub.common.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://all-in-one-cs2.vercel.app}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        // Health and monitoring
                        .requestMatchers("/api/v1/health/**").permitAll()
                        // Authentication endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // OpenAPI / Swagger UI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Public GET endpoints for minigames & loadout previews
                        .requestMatchers(HttpMethod.GET, "/api/v1/minigames/**").permitAll()
                        // Public daily guess endpoint for zero-friction play (supports guest session header)
                        .requestMatchers(HttpMethod.POST, "/api/v1/minigames/daily/guess").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/loadouts/**").permitAll()
                        // Public loadouts generator endpoint for zero login friction
                        .requestMatchers(HttpMethod.POST, "/api/v1/loadouts/generate").permitAll()
                        // Public GET endpoints for stratboard utility lineups and tactics playbooks
                        .requestMatchers(HttpMethod.GET, "/api/v1/stratboard/**").permitAll()
                        // WebSocket STOMP endpoint for real-time collaborative stratboard
                        .requestMatchers("/ws/stratboard/**").permitAll()
                        // Stratboard room save-tactic requires authentication, while room joining/snapshotting is zero-friction
                        .requestMatchers("/api/v1/stratboard/rooms/*/save-tactic").authenticated()
                        .requestMatchers("/api/v1/stratboard/rooms/**").permitAll()
                        // Matches: Public access for match submission, simulation, scoreboard viewing, and roast cards
                        .requestMatchers("/api/v1/matches/**").permitAll()
                        // Internal worker callback endpoints guarded by X-Worker-Secret header check
                        .requestMatchers("/api/v1/internal/matches/**").permitAll()
                        // User profile rules: /me requires authentication, /api/v1/users/{userId} is public
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDto error = ErrorResponseDto.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Authentication is required to access this resource",
                    List.of(request.getRequestURI())
                );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Ensure wildcard pattern for Vercel preview deployments
        if (!origins.contains("https://*.vercel.app")) {
            origins.add("https://*.vercel.app");
        }

        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Guest-Session-Id",
                "X-Worker-Secret",
                "Accept",
                "Origin",
                "X-Requested-With",
                "*"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Guest-Session-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
