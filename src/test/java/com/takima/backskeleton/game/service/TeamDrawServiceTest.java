package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class TeamDrawServiceTest {
    private static final int DRAWS = 100_000;
    private static final double TOLERANCE = 0.25;

    private final DraftService draftService = GameTestContext.draftService();
    private final TeamSeasonCatalog catalog = GameTestContext.catalog();

    @Test
    void everyDrawableTeamIsDrawnWithTheSameProbability() {
        Map<Integer, Long> drawsByTeam = LongStream.range(0, DRAWS)
                .mapToObj(seed -> draftService.start(Formation.FOUR_THREE_THREE, seed))
                .map(Game::currentDraw)
                .collect(Collectors.groupingBy(key -> key.teamId(), Collectors.counting()));
        long teamCount = catalog.drawableTeamSeasons().stream().map(TeamSeason::teamId).distinct().count();
        double expected = (double) DRAWS / teamCount;

        assertThat(drawsByTeam).hasSize((int) teamCount);
        assertThat(drawsByTeam.values()).allSatisfy(count -> assertThat(count).isBetween(
                (long) (expected * (1 - TOLERANCE)), (long) (expected * (1 + TOLERANCE))));
    }

    @Test
    void winningSeasonsAreNotFavouredWithinTheDraw() {
        long winnerDraws = LongStream.range(0, DRAWS)
                .mapToObj(seed -> draftService.start(Formation.FOUR_THREE_THREE, seed))
                .map(game -> catalog.find(game.currentDraw()).orElseThrow())
                .filter(teamSeason -> teamSeason.roundReached() == Round.WINNER)
                .count();
        long winnerTeams = catalog.drawableTeamSeasons().stream()
                .filter(teamSeason -> teamSeason.roundReached() == Round.WINNER)
                .map(TeamSeason::teamId).distinct().count();
        long teams = catalog.drawableTeamSeasons().stream().map(TeamSeason::teamId).distinct().count();

        assertThat(winnerDraws).isLessThan((long) (DRAWS * ((double) winnerTeams / teams) * (1 + TOLERANCE)));
    }
}
