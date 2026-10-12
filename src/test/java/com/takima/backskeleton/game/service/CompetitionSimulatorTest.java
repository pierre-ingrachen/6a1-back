package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.GroupStanding;
import com.takima.backskeleton.game.model.MatchResult;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamStrength;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionSimulatorTest {
    private static final int RUNS = 300;
    private static final TeamStrength SQUAD = new TeamStrength(82, 82);

    private final CompetitionSimulator simulator = GameTestContext.competitionSimulator();

    @Test
    void sameSeedGivesSameCompetition() {
        assertThat(simulator.simulate(SQUAD, 5L)).isEqualTo(simulator.simulate(SQUAD, 5L));
    }

    @Test
    void groupStageHasSixMatchesAgainstThreeOpponents() {
        CompetitionResult result = simulator.simulate(SQUAD, 1L);

        assertThat(result.groupStage().matches()).hasSize(6);
        assertThat(result.groupStage().matches().stream().map(match -> match.opponent().teamId()).distinct()).hasSize(3);
        assertThat(result.groupStage().standings()).hasSize(4);
        assertThat(result.groupStage().rank()).isBetween(1, 4);
    }

    @Test
    void groupStandingsAreSortedByPoints() {
        List<GroupStanding> standings = simulator.simulate(SQUAD, 2L).groupStage().standings();

        assertThat(standings).extracting(GroupStanding::points).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(standings).allSatisfy(standing -> assertThat(standing.played()).isEqualTo(6));
    }

    @Test
    void playerAlwaysQualifiesForKnockoutStage() {
        assertThat(allResults())
                .allSatisfy(result -> assertThat(result.knockoutMatches()).isNotEmpty())
                .allSatisfy(result -> assertThat(result.knockoutMatches().get(0).round()).isEqualTo(Round.ROUND_OF_16));
    }

    @Test
    void knockoutRoundsFollowTheBracketOrder() {
        assertThat(allResults()).allSatisfy(result -> {
            List<Round> played = result.knockoutMatches().stream().map(MatchResult::round).toList();
            assertThat(played).isEqualTo(Round.KNOCKOUT_ROUNDS.subList(0, played.size()));
        });
    }

    @Test
    void playerStopsAtTheFirstDefeat() {
        assertThat(allResults()).allSatisfy(result -> {
            List<MatchResult> matches = result.knockoutMatches();
            assertThat(matches.subList(0, matches.size() - 1)).allMatch(MatchResult::won);
            if (!result.champion()) {
                assertThat(matches.get(matches.size() - 1).won()).isFalse();
                assertThat(result.outcome()).isEqualTo(matches.get(matches.size() - 1).round());
            }
        });
    }

    @Test
    void championWonAllFourKnockoutMatches() {
        List<CompetitionResult> champions = allResults().stream().filter(CompetitionResult::champion).toList();

        assertThat(champions).isNotEmpty().allSatisfy(result -> {
            assertThat(result.outcome()).isEqualTo(Round.WINNER);
            assertThat(result.knockoutMatches()).hasSize(4).allMatch(MatchResult::won);
        });
    }

    @Test
    void opponentsHaveReallyReachedTheRoundTheyFace() {
        Map<String, TeamSeason> bySeason = GameTestContext.catalog().teamSeasons().stream()
                .collect(Collectors.toMap(team -> team.teamId() + "/" + team.seasonLabel(), Function.identity()));

        assertThat(allResults()).allSatisfy(result -> result.knockoutMatches().forEach(match -> {
            Round reached = bySeason.get(match.opponent().teamId() + "/" + match.opponent().seasonLabel()).roundReached();
            assertThat(reached.hasPlayed(match.round())).as(match.round() + " vs " + match.opponent()).isTrue();
        }));
    }

    @Test
    void sameClubIsNeverFacedTwice() {
        assertThat(allResults()).allSatisfy(result -> {
            List<Integer> clubs = new ArrayList<>(result.groupStage().matches().stream()
                    .map(match -> match.opponent().teamId()).distinct().toList());
            clubs.addAll(result.knockoutMatches().stream().map(match -> match.opponent().teamId()).toList());
            assertThat(clubs).doesNotHaveDuplicates();
        });
    }

    @Test
    void knockoutMatchesAlwaysHaveAWinner() {
        assertThat(allResults()).allSatisfy(result -> result.knockoutMatches().forEach(match -> {
            boolean decidedOnGoals = match.goalsFor() != match.goalsAgainst();
            boolean decidedOnPenalties = match.penaltiesFor() != null && !match.penaltiesFor().equals(match.penaltiesAgainst());
            assertThat(decidedOnGoals || decidedOnPenalties).isTrue();
        }));
    }

    @Test
    void betterGroupRankMeetsWeakerRoundOfSixteenOpponentsOnAverage() {
        double firstPlaceAverage = averageRoundOfSixteenRating(rank -> rank == 1);
        double lastPlaceAverage = averageRoundOfSixteenRating(rank -> rank >= 3);

        assertThat(firstPlaceAverage).isLessThan(lastPlaceAverage);
    }

    private double averageRoundOfSixteenRating(IntPredicate rankFilter) {
        return IntStream.range(0, 1500)
                .mapToObj(seed -> simulator.simulate(SQUAD, seed))
                .filter(result -> rankFilter.test(result.groupStage().rank()))
                .mapToInt(result -> result.knockoutMatches().get(0).opponent().rating())
                .average()
                .orElseThrow();
    }

    private List<CompetitionResult> allResults() {
        return IntStream.range(0, RUNS).mapToObj(seed -> simulator.simulate(SQUAD, seed)).toList();
    }
}
