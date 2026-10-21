package com.takima.backskeleton.game.dto;

import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull(message = "fromSlot is required") Integer fromSlot,
        @NotNull(message = "toSlot is required") Integer toSlot) {
}
