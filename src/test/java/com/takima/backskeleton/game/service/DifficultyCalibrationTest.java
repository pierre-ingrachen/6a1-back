package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamStrength;
import com.takima.backskeleton.game.support.DraftBot;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyCalibrationTest {
    private static final int SIMULATIONS = 1000;
    private static final int STAR_RATING_THRESHOLD = 85;

    private final DraftService draftService = GameTestContext.draftService();
    private final TeamStrengthService strengthService = GameTestContext.strengthService();
    private final CompetitionSimulator competitionSimulator = GameTestContext.competitionSimulator();

    @Test
    void starSeekingPlayerWinsAboutOneGameInFive() {
        double titleRate = titleRateOfDrafts(Formation.FOUR_THREE_THREE, (bot, game) -> bot.playStarSeeker(game, STAR_RATING_THRESHOLD));

        assertThat(titleRate).isBetween(0.12, 0.28);
    }

    @Test
    void starSeekingPlayerWinsInEveryFormationAtSimilarRate() {
        Arrays.stream(Formation.values()).forEach(formation -> {
            double titleRate = titleRateOfDrafts(formation, (bot, game) -> bot.playStarSeeker(game, STAR_RATING_THRESHOLD));

            assertThat(titleRate).as(formation.label()).isBetween(0.12, 0.32);
        });
    }

    @Test
    void randomPickerAlmostNeverWins() {
        double titleRate = titleRateOfDrafts(Formation.FOUR_THREE_THREE, (bot, game) -> bot.playRandomly(game));

        assertThat(titleRate).isLessThan(0.02);
    }

    @Test
    void dreamTeamWinsOftenButNotAlways() {
        double titleRate = titleRateOfStrength(new TeamStrength(90, 90));

        assertThat(titleRate).isBetween(0.35, 0.65);
    }

    @Test
    void hopelessTeamAlmostNeverWins() {
        double titleRate = titleRateOfStrength(new TeamStrength(71, 71));

        assertThat(titleRate).isLessThan(0.005);
    }

    @Test
    void nobodyIsEliminatedInGroupStage() {
        long eliminatedInGroups = IntStream.range(0, SIMULATIONS)
                .mapToObj(seed -> competitionSimulator.simulate(new TeamStrength(70, 70), seed))
                .filter(result -> result.outcome() == Round.GROUP_STAGE)
                .count();

        assertThat(eliminatedInGroups).isZero();
    }

    private double titleRateOfDrafts(Formation formation, DraftStrategy strategy) {
        long titles = IntStream.range(0, SIMULATIONS)
                .mapToObj(seed -> {
                    Random random = new Random(seed);
                    Game drafted = strategy.play(
                            new DraftBot(draftService, GameTestContext.eligibility(), random),
                            draftService.start(formation, seed));
                    TeamStrength squad = strengthService.ofSquad(drafted.formation(), drafted.picks());
                    return competitionSimulator.simulate(squad, seed);
                })
                .filter(CompetitionResult::champion)
                .count();
        return (double) titles / SIMULATIONS;
    }

    private double titleRateOfStrength(TeamStrength strength) {
        long titles = IntStream.range(0, SIMULATIONS * 2)
                .mapToObj(seed -> competitionSimulator.simulate(strength, seed))
                .filter(CompetitionResult::champion)
                .count();
        return (double) titles / (SIMULATIONS * 2);
    }

    @FunctionalInterface
    private interface DraftStrategy {
        Game play(DraftBot bot, Game game);
    }
}
