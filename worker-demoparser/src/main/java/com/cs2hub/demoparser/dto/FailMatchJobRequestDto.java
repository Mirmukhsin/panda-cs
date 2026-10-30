package com.cs2hub.demoparser.dto;

import java.io.Serializable;

public record FailMatchJobRequestDto(
        String reason
) implements Serializable {}
