package com.cs2hub.demoparser.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class DemoFileDownloader {

    private final long maxFileSizeBytes;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public DemoFileDownloader(
            @Value("${worker.max-file-size-bytes:524288000}") long maxFileSizeBytes,
            @Value("${worker.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${worker.read-timeout-ms:30000}") int readTimeoutMs
    ) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public Path downloadDemo(String demoUrl) throws IOException {
        if (demoUrl == null || demoUrl.isBlank()) {
            return null;
        }

        Path tempFile = Files.createTempFile("cs2-demo-", ".dem");
        try {
            if (demoUrl.startsWith("http://") || demoUrl.startsWith("https://")) {
                downloadHttp(demoUrl, tempFile);
            } else if (demoUrl.startsWith("file:")) {
                Path source = Paths.get(URI.create(demoUrl));
                validateAndCopyLocal(source, tempFile);
            } else {
                Path source = Paths.get(demoUrl);
                validateAndCopyLocal(source, tempFile);
            }
            log.info("Downloaded demo to temp file: {} (size: {} bytes)", tempFile, Files.size(tempFile));
            return tempFile;
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupEx) {
                log.warn("Failed to clean up temp file after download error: {}", tempFile, cleanupEx);
            }
            throw ex;
        }
    }

    private void downloadHttp(String demoUrl, Path tempFile) throws IOException {
        URLConnection connection = URI.create(demoUrl).toURL().openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);

        long contentLength = connection.getContentLengthLong();
        if (contentLength > maxFileSizeBytes) {
            throw new IllegalArgumentException("Demo file size exceeds maximum limit of " + maxFileSizeBytes + " bytes");
        }

        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(tempFile)) {
            byte[] buffer = new byte[16384];
            int read;
            long totalRead = 0;
            while ((read = in.read(buffer)) != -1) {
                totalRead += read;
                if (totalRead > maxFileSizeBytes) {
                    throw new IllegalArgumentException("Downloaded stream exceeded maximum limit of " + maxFileSizeBytes + " bytes");
                }
                out.write(buffer, 0, read);
            }
        }
    }

    private void validateAndCopyLocal(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source demo file not found: " + source);
        }
        long size = Files.size(source);
        if (size > maxFileSizeBytes) {
            throw new IllegalArgumentException("Source demo file size exceeds maximum limit: " + size + " > " + maxFileSizeBytes);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
