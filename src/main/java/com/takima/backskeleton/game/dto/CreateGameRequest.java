package com.takima.backskeleton.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(
        @NotBlank(message = "formation is required")
        @Size(max = 20, message = "formation must be at most 20 characters")
        String formation) {
}
