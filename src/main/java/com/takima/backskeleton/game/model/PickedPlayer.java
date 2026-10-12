package com.takima.backskeleton.game.model;

import java.util.Set;

public record PickedPlayer(
        int playerId,
        String name,
        int rating,
        String teamName,
        String seasonLabel,
        Set<FieldPosition> playablePositions
) {
    public boolean canPlay(FieldPosition position) {
        return playablePositions.contains(position);
    }
}
