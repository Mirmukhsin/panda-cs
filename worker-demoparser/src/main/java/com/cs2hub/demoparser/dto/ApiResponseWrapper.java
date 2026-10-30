package com.cs2hub.demoparser.dto;

import java.io.Serializable;
import java.time.Instant;

public record ApiResponseWrapper<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) implements Serializable {}
