package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TeamSeasonCatalogTest {
    private static final int INTER_ID = 75;

    private final TeamSeasonCatalog catalog = GameTestContext.catalog();

    @Test
    void drawableTeamSeasonsAllReachedTheRoundOfSixteen() {
        assertThat(catalog.drawableTeamSeasons())
                .allMatch(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16));
    }

    @Test
    void eachTeamKeepsTheCeilingOfFortyPercentOfItsQualifyingEditions() {
        Map<Integer, Long> qualifying = catalog.teamSeasons().stream()
                .filter(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16))
                .collect(Collectors.groupingBy(TeamSeason::teamId, Collectors.counting()));
        Map<Integer, Long> kept = catalog.drawableTeamSeasons().stream()
                .collect(Collectors.groupingBy(TeamSeason::teamId, Collectors.counting()));

        qualifying.forEach((teamId, count) ->
                assertThat(kept.get(teamId)).as("team " + teamId).isEqualTo((count * 40 + 99) / 100));
    }

    @Test
    void sixQualifyingEditionsKeepThree() {
        List<TeamSeason> inter = catalog.drawableTeamSeasons().stream()
                .filter(teamSeason -> teamSeason.teamId() == INTER_ID)
                .toList();

        assertThat(inter).extracting(TeamSeason::startYear).containsExactlyInAnyOrder(2024, 2022, 2010);
    }

    @Test
    void keptEditionsAreTheStrongestOnes() {
        TeamStrengthService strength = GameTestContext.strengthService();
        List<TeamSeason> kept = catalog.drawableTeamSeasons();
        List<TeamSeason> dropped = catalog.teamSeasons().stream()
                .filter(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16))
                .filter(teamSeason -> !kept.contains(teamSeason))
                .toList();

        dropped.forEach(teamSeason -> kept.stream()
                .filter(other -> other.teamId() == teamSeason.teamId())
                .forEach(other -> assertThat(strength.ofOpponent(other).overall())
                        .isGreaterThanOrEqualTo(strength.ofOpponent(teamSeason).overall())));
    }
}
