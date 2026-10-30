package com.cs2hub.matches.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Worker report for failed demo parsing job")
public record FailMatchJobRequestDto(
        @Schema(description = "Diagnostic reason for parser failure", example = "Corrupt demo header or unsupported protocol version")
        String reason
) implements Serializable {}
