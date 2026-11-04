package com.cs2hub.loadouts.controller;

import com.cs2hub.common.exception.GlobalExceptionHandler;
import com.cs2hub.common.security.CurrentUserArgumentResolver;
import com.cs2hub.loadouts.service.EventsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventsControllerTest {

    @Spy
    private EventsService eventsService = new EventsService();

    @InjectMocks
    private EventsController eventsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/events/hub should return 200 with 5 giveaways and >= 8 discounts")
    void shouldReturnEventsHubPayload() throws Exception {
        mockMvc.perform(get("/api/v1/events/hub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.giveaways", hasSize(5)))
                .andExpect(jsonPath("$.data.giveaways[0].id").exists())
                .andExpect(jsonPath("$.data.giveaways[0].title").exists())
                .andExpect(jsonPath("$.data.giveaways[0].valueUsd").exists())
                .andExpect(jsonPath("$.data.discounts", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$.data.discounts[0].originalPrice").exists())
                .andExpect(jsonPath("$.data.discounts[0].salePrice").exists())
                .andExpect(jsonPath("$.data.discounts[0].discountPercent").exists())
                .andExpect(jsonPath("$.data.specialEvents").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/events/giveaways/{id}/enter should increment entries and return 200")
    void shouldEnterGiveawaySuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/events/giveaways/giveaway-ak47-bloodsport/enter")
                        .header("X-Guest-Session-Id", "guest-test-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.giveawayId").value("giveaway-ak47-bloodsport"))
                .andExpect(jsonPath("$.data.entered").value(true))
                .andExpect(jsonPath("$.data.totalEntries").value(1241));

        // Entering again with the same guest session should be idempotent
        mockMvc.perform(post("/api/v1/events/giveaways/giveaway-ak47-bloodsport/enter")
                        .header("X-Guest-Session-Id", "guest-test-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEntries").value(1241));
    }

    @Test
    @DisplayName("POST /api/v1/events/giveaways/{id}/enter should return 404 for unknown giveaway")
    void shouldReturn404ForUnknownGiveaway() throws Exception {
        mockMvc.perform(post("/api/v1/events/giveaways/unknown-giveaway-id/enter"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
