package com.takima.backskeleton.game.model;

import java.util.Set;

public record SeasonPlayer(
        int playerId,
        String name,
        int rating,
        Line ratingLine,
        Set<FieldPosition> playablePositions,
        int minutes
) {
    public boolean canPlay(FieldPosition position) {
        return playablePositions.contains(position);
    }
}
