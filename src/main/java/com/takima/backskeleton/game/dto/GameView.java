package com.takima.backskeleton.game.dto;

import com.takima.backskeleton.game.model.Line;

import java.util.List;
import java.util.UUID;

public record GameView(
        UUID id,
        String formation,
        GameStatus status,
        int picksRemaining,
        int redrawsRemaining,
        Double squadAverageRating,
        List<SlotView> slots,
        DrawView draw
) {
    public record SlotView(int index, String position, Line line, PickedPlayerView player) {
    }

    public record PickedPlayerView(
            int playerId,
            String name,
            int rating,
            String teamName,
            String seasonLabel,
            List<String> positions
    ) {
    }

    public record DrawView(
            int teamId,
            String teamName,
            String country,
            String seasonLabel,
            boolean seasonRedrawAvailable,
            boolean teamRedrawAvailable,
            List<DrawPlayerView> players
    ) {
    }

    public record DrawPlayerView(
            int playerId,
            String name,
            int rating,
            Line line,
            List<String> positions,
            List<Integer> eligibleSlots,
            boolean alreadyPicked
    ) {
    }
}
