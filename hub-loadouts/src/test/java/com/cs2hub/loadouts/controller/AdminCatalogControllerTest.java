package com.cs2hub.loadouts.controller;

import com.cs2hub.common.dto.ApiResponseDto;
import com.cs2hub.loadouts.dto.SeedSummaryDto;
import com.cs2hub.loadouts.service.CatalogSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogControllerTest {

    @Mock
    private CatalogSyncService catalogSyncService;

    private AdminCatalogController adminCatalogController;

    @BeforeEach
    void setUp() {
        adminCatalogController = new AdminCatalogController(catalogSyncService);
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/admin/seed should invoke seedCuratedCatalog and return 200 OK")
    void shouldSeedCuratedCatalogSuccessfully() {
        SeedSummaryDto summary = new SeedSummaryDto(29, 10, "Successfully seeded 29 curated skins and 10 stickers");
        when(catalogSyncService.seedCuratedCatalog()).thenReturn(summary);

        ResponseEntity<ApiResponseDto<SeedSummaryDto>> response = adminCatalogController.seedCatalog();

        verify(catalogSyncService).seedCuratedCatalog();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().skinsCount()).isEqualTo(29);
        assertThat(response.getBody().data().stickersCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /api/v1/loadouts/admin/sync should invoke syncExternalCatalog and return 200 OK")
    void shouldSyncExternalCatalogSuccessfully() {
        SeedSummaryDto summary = new SeedSummaryDto(150, 40, "Catalog sync complete. Synced 150 skins and 40 stickers.");
        when(catalogSyncService.syncExternalCatalog()).thenReturn(summary);

        ResponseEntity<ApiResponseDto<SeedSummaryDto>> response = adminCatalogController.syncCatalog();

        verify(catalogSyncService).syncExternalCatalog();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().skinsCount()).isEqualTo(150);
        assertThat(response.getBody().data().stickersCount()).isEqualTo(40);
    }
}
