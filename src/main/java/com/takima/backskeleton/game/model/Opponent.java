package com.takima.backskeleton.game.model;

public record Opponent(TeamSeason teamSeason, TeamStrength strength) {
    public OpponentSummary summary() {
        return new OpponentSummary(
                teamSeason.teamId(),
                teamSeason.teamName(),
                teamSeason.seasonLabel(),
                (int) Math.round(strength.overall()));
    }
}
