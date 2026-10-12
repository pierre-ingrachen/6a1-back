package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.MatchOutcome;
import com.takima.backskeleton.game.model.TeamStrength;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MatchSimulatorTest {
    private static final int MATCHES = 5000;
    private static final TeamStrength STRONG = new TeamStrength(88, 88);
    private static final TeamStrength AVERAGE = new TeamStrength(75, 75);

    private final MatchSimulator simulator = new MatchSimulator();

    @Test
    void sameSeedGivesSameMatch() {
        MatchOutcome first = simulator.playKnockoutMatch(STRONG, AVERAGE, new Random(42));
        MatchOutcome second = simulator.playKnockoutMatch(STRONG, AVERAGE, new Random(42));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void strongTeamWinsMostGroupMatches() {
        long wins = IntStream.range(0, MATCHES)
                .mapToObj(seed -> simulator.playGroupMatch(STRONG, AVERAGE, new Random(seed)))
                .filter(MatchOutcome::winnerIsA)
                .count();

        assertThat((double) wins / MATCHES).isBetween(0.65, 0.95);
    }

    @Test
    void underdogStillWinsSometimes() {
        long underdogWins = IntStream.range(0, MATCHES)
                .mapToObj(seed -> simulator.playGroupMatch(AVERAGE, STRONG, new Random(seed)))
                .filter(MatchOutcome::winnerIsA)
                .count();

        assertThat((double) underdogWins / MATCHES).isBetween(0.02, 0.2);
    }

    @Test
    void equalTeamsAreBalanced() {
        long wins = IntStream.range(0, MATCHES)
                .mapToObj(seed -> simulator.playGroupMatch(AVERAGE, AVERAGE, new Random(seed)))
                .filter(MatchOutcome::winnerIsA)
                .count();

        assertThat((double) wins / MATCHES).isBetween(0.33, 0.47);
    }

    @Test
    void groupMatchesCanBeDrawn() {
        boolean anyDraw = IntStream.range(0, 200)
                .mapToObj(seed -> simulator.playGroupMatch(AVERAGE, AVERAGE, new Random(seed)))
                .anyMatch(MatchOutcome::isDraw);

        assertThat(anyDraw).isTrue();
    }

    @Test
    void knockoutMatchAlwaysHasAWinner() {
        boolean everyMatchDecided = IntStream.range(0, MATCHES)
                .mapToObj(seed -> simulator.playKnockoutMatch(AVERAGE, AVERAGE, new Random(seed)))
                .noneMatch(MatchOutcome::isDraw);

        assertThat(everyMatchDecided).isTrue();
    }

    @Test
    void penaltiesOnlyHappenAfterExtraTimeDraw() {
        IntStream.range(0, MATCHES)
                .mapToObj(seed -> simulator.playKnockoutMatch(AVERAGE, AVERAGE, new Random(seed)))
                .filter(outcome -> outcome.penalties() != null)
                .forEach(outcome -> {
                    assertThat(outcome.extraTime()).isTrue();
                    assertThat(outcome.goalsA()).isEqualTo(outcome.goalsB());
                    assertThat(outcome.penalties().scoredA()).isNotEqualTo(outcome.penalties().scoredB());
                });
    }
}
