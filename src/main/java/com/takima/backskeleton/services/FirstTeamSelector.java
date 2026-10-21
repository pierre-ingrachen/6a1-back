package com.takima.backskeleton.services;

import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.models.PlayerSeasonStats;

import java.util.Comparator;
import java.util.List;

final class FirstTeamSelector {
    private FirstTeamSelector() {
    }

    static SeasonPlayerStats select(List<PlayerSeasonStats> teamsOfSeason) {
        if (teamsOfSeason.size() == 1) {
            return new SeasonPlayerStats(teamsOfSeason.get(0), false);
        }
        List<PlayerSeasonStats> eliminatedInGroupStage = teamsOfSeason.stream()
                .filter(stats -> Round.isGroupStageLabel(stats.eliminationRound()))
                .toList();
        if (eliminatedInGroupStage.size() == 1) {
            return new SeasonPlayerStats(eliminatedInGroupStage.get(0), false);
        }
        PlayerSeasonStats mostMatchesPlayed = teamsOfSeason.stream()
                .max(Comparator.comparingInt(PlayerSeasonStats::matchesPlayed).thenComparingInt(PlayerSeasonStats::minutes))
                .orElseThrow();
        return new SeasonPlayerStats(mostMatchesPlayed, true);
    }
}
