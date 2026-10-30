package com.cs2hub.demoparser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoFileDownloaderTest {

    private DemoFileDownloader downloader;

    @BeforeEach
    void setUp() {
        downloader = new DemoFileDownloader(10_000_000L, 2000, 5000);
    }

    @Test
    @DisplayName("Should return null when demo URL is null or blank")
    void shouldReturnNullWhenUrlIsBlank() throws IOException {
        assertThat(downloader.downloadDemo(null)).isNull();
        assertThat(downloader.downloadDemo("   ")).isNull();
    }

    @Test
    @DisplayName("Should copy local file when file URI or local path is given")
    void shouldDownloadLocalFile() throws IOException {
        Path sourceFile = Files.createTempFile("source-cs2-", ".dem");
        try {
            Files.writeString(sourceFile, "DUMMY_DEMO_DATA_PBDEMS2");

            Path downloaded = downloader.downloadDemo(sourceFile.toUri().toString());
            try {
                assertThat(downloaded).isNotNull();
                assertThat(Files.exists(downloaded)).isTrue();
                assertThat(Files.readString(downloaded)).isEqualTo("DUMMY_DEMO_DATA_PBDEMS2");
            } finally {
                if (downloaded != null) {
                    Files.deleteIfExists(downloaded);
                }
            }
        } finally {
            Files.deleteIfExists(sourceFile);
        }
    }

    @Test
    @DisplayName("Should throw exception when source file does not exist")
    void shouldThrowExceptionWhenFileNotFound() {
        assertThatThrownBy(() -> downloader.downloadDemo("file:///non-existent-demo-file.dem"))
                .isInstanceOf(Exception.class);
    }
}
