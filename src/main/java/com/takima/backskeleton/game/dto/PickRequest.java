package com.takima.backskeleton.game.dto;

import jakarta.validation.constraints.NotNull;

public record PickRequest(
        @NotNull(message = "playerId is required") Integer playerId,
        @NotNull(message = "slotIndex is required") Integer slotIndex) {
}
