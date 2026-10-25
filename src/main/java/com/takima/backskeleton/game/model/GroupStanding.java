package com.takima.backskeleton.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GroupStanding(
        String teamName,
        String seasonLabel,
        boolean player,
        int played,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int points
) {
    private static final int POINTS_FOR_WIN = 3;

    public static GroupStanding empty(String teamName, String seasonLabel, boolean player) {
        return new GroupStanding(teamName, seasonLabel, player, 0, 0, 0, 0, 0, 0, 0);
    }

    public GroupStanding withMatch(int scored, int conceded) {
        boolean won = scored > conceded;
        boolean drawn = scored == conceded;
        return new GroupStanding(
                teamName,
                seasonLabel,
                player,
                played + 1,
                wins + (won ? 1 : 0),
                draws + (drawn ? 1 : 0),
                losses + (!won && !drawn ? 1 : 0),
                goalsFor + scored,
                goalsAgainst + conceded,
                points + (won ? POINTS_FOR_WIN : drawn ? 1 : 0));
    }

    @JsonProperty("goalDifference")
    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}
