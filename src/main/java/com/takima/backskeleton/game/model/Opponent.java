package com.takima.backskeleton.game.model;

import java.util.Comparator;

public record Opponent(TeamSeason teamSeason, TeamStrength strength) {
    private static final int SUMMARY_PLAYERS_LIMIT = 14;

    public OpponentSummary summary() {
        return new OpponentSummary(
                teamSeason.teamId(),
                teamSeason.teamName(),
                teamSeason.seasonLabel(),
                (int) Math.round(strength.overall()),
                teamSeason.players().stream()
                        .sorted(Comparator.comparingInt(SeasonPlayer::minutes).reversed())
                        .limit(SUMMARY_PLAYERS_LIMIT)
                        .map(OpponentPlayer::from)
                        .toList());
    }
}
