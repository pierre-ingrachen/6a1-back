package com.takima.backskeleton.services;

import com.takima.backskeleton.models.PlayerSeasonStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FirstTeamSelectorTest {
    @Test
    void singleTeamIsNotEstimated() {
        SeasonPlayerStats selected = FirstTeamSelector.select(List.of(stint("Only", "Finale", 5, 400)));

        assertThat(selected.stats().teamName()).isEqualTo("Only");
        assertThat(selected.teamEstimated()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Phase de groupes", "Phase de ligue", "Barrages"})
    void teamEliminatedBeforeTheKnockoutsIsTheFirstTeam(String groupStageLabel) {
        SeasonPlayerStats selected = FirstTeamSelector.select(List.of(
                stint("Late", "Quarts de finale", 7, 630),
                stint("Early", groupStageLabel, 4, 360)));

        assertThat(selected.stats().teamName()).isEqualTo("Early");
        assertThat(selected.teamEstimated()).isFalse();
    }

    @Test
    void fallsBackToMostMatchesAndFlagsEstimation() {
        SeasonPlayerStats selected = FirstTeamSelector.select(List.of(
                stint("Semi", "Demi-finales", 5, 400),
                stint("Sixteen", "Huitièmes de finale", 2, 180)));

        assertThat(selected.stats().teamName()).isEqualTo("Semi");
        assertThat(selected.teamEstimated()).isTrue();
    }

    private static PlayerSeasonStats stint(String team, String round, int matches, int minutes) {
        return new PlayerSeasonStats(1, "Player", "Forward", null, null, team, round, null, null,
                Map.of("minutes", (double) minutes, "matchs_joues", (double) matches));
    }
}
