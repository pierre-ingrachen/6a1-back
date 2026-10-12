package com.takima.backskeleton.game.model;

import java.util.List;

public record TeamSeason(
        int teamId,
        String teamName,
        String country,
        int startYear,
        String seasonLabel,
        Round roundReached,
        List<SeasonPlayer> players
) {
    public TeamSeasonKey key() {
        return new TeamSeasonKey(teamId, startYear);
    }
}
