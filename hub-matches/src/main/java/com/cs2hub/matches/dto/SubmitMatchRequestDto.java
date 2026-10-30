package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

@Schema(description = "Request body to submit a CS2 match demo share code for parsing")
public record SubmitMatchRequestDto(
        @NotBlank
        @Schema(description = "Match share code or demo URL / hash", example = "CSGO-45s7a-8fbc2-99m12-abcde-12345")
        String shareCode,

        @Schema(description = "Direct HTTP/S3 URL to download the .dem match demo file")
        String demoUrl
) implements Serializable {}
