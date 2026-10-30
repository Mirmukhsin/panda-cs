package com.cs2hub.stratboard.domain;

import lombok.Getter;

@Getter
public enum ThrowType {
    JUMP_THROW("Jump Throw"),
    STANDING("Standing Throw"),
    RUN_THROW("Running Throw"),
    LEFT_RIGHT_CLICK("Left + Right Click"),
    CROUCH_THROW("Crouch Throw");

    private final String displayName;

    ThrowType(String displayName) {
        this.displayName = displayName;
    }
}
