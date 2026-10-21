package com.takima.backskeleton.game.dto;

import com.takima.backskeleton.game.model.RedrawType;
import jakarta.validation.constraints.NotNull;

public record RedrawRequest(@NotNull(message = "type is required") RedrawType type) {
}
