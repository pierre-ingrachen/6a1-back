package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.GroupStageResult;
import com.takima.backskeleton.game.model.GroupStanding;
import com.takima.backskeleton.game.model.MatchOutcome;
import com.takima.backskeleton.game.model.MatchResult;
import com.takima.backskeleton.game.model.Opponent;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamStrength;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class CompetitionSimulator {
    private static final int GROUP_TIERS = 3;
    private static final int MATCHES_PER_GROUP_PAIRING = 2;
    private static final int PLAYER_INDEX = 0;
    private static final Map<Integer, RankWindow> ROUND_OF_16_WINDOWS = Map.of(
            1, new RankWindow(0.0, 0.6),
            2, new RankWindow(0.2, 0.8),
            3, new RankWindow(0.4, 1.0),
            4, new RankWindow(0.4, 1.0));

    private final TeamSeasonCatalog catalog;
    private final MatchSimulator matchSimulator;

    public CompetitionSimulator(TeamSeasonCatalog catalog, MatchSimulator matchSimulator) {
        this.catalog = catalog;
        this.matchSimulator = matchSimulator;
    }

    public CompetitionResult simulate(TeamStrength squad, long seed) {
        Random random = new Random(seed);
        TeamStrength effective = new TeamStrength(
                squad.attack() - GameBalance.PLAYER_HANDICAP,
                squad.defense() - GameBalance.PLAYER_HANDICAP);
        Set<Integer> usedClubs = new HashSet<>();

        List<Opponent> groupOpponents = drawGroupOpponents(random, usedClubs);
        GroupStageResult groupStage = playGroupStage(effective, groupOpponents, random);

        List<MatchResult> knockoutMatches = new ArrayList<>();
        for (Round round : Round.KNOCKOUT_ROUNDS) {
            Opponent opponent = drawKnockoutOpponent(round, groupStage.rank(), usedClubs, random);
            MatchOutcome outcome = matchSimulator.playKnockoutMatch(effective, opponent.strength(), random);
            knockoutMatches.add(MatchResult.of(round, opponent.summary(), outcome));
            if (!outcome.winnerIsA()) {
                return new CompetitionResult(squad, groupStage, knockoutMatches, round, false);
            }
        }
        return new CompetitionResult(squad, groupStage, knockoutMatches, Round.WINNER, true);
    }

    private List<Opponent> drawGroupOpponents(Random random, Set<Integer> usedClubs) {
        List<Opponent> sorted = sortedByStrength(catalog.opponents());
        int tierSize = sorted.size() / GROUP_TIERS;
        return IntStream.range(0, GROUP_TIERS)
                .mapToObj(tier -> sorted.subList(tier * tierSize, tier == GROUP_TIERS - 1 ? sorted.size() : (tier + 1) * tierSize))
                .map(tierOpponents -> drawUnusedClub(tierOpponents, usedClubs, random))
                .toList();
    }

    private Opponent drawKnockoutOpponent(Round round, int groupRank, Set<Integer> usedClubs, Random random) {
        List<Opponent> reachedRound = sortedByStrength(catalog.opponents().stream()
                .filter(opponent -> opponent.teamSeason().roundReached().hasPlayed(round))
                .toList());
        List<Opponent> pool = round == Round.ROUND_OF_16
                ? ROUND_OF_16_WINDOWS.get(groupRank).slice(reachedRound)
                : reachedRound;
        return drawUnusedClub(pool, usedClubs, random);
    }

    private Opponent drawUnusedClub(List<Opponent> candidates, Set<Integer> usedClubs, Random random) {
        List<Opponent> available = candidates.stream()
                .filter(opponent -> !usedClubs.contains(opponent.teamSeason().teamId()))
                .toList();
        List<Opponent> source = available.isEmpty() ? candidates : available;
        Opponent drawn = source.get(random.nextInt(source.size()));
        usedClubs.add(drawn.teamSeason().teamId());
        return drawn;
    }

    private GroupStageResult playGroupStage(TeamStrength squad, List<Opponent> opponents, Random random) {
        List<GroupStanding> standings = new ArrayList<>();
        standings.add(GroupStanding.empty("Your team", "", true));
        opponents.forEach(opponent -> standings.add(
                GroupStanding.empty(opponent.teamSeason().teamName(), opponent.teamSeason().seasonLabel(), false)));

        List<MatchResult> playerMatches = new ArrayList<>();
        for (int index = 0; index < opponents.size(); index++) {
            Opponent opponent = opponents.get(index);
            for (int leg = 0; leg < MATCHES_PER_GROUP_PAIRING; leg++) {
                MatchOutcome outcome = matchSimulator.playGroupMatch(squad, opponent.strength(), random);
                playerMatches.add(MatchResult.of(Round.GROUP_STAGE, opponent.summary(), outcome));
                record(standings, PLAYER_INDEX, index + 1, outcome);
            }
        }
        for (int first = 0; first < opponents.size(); first++) {
            for (int second = first + 1; second < opponents.size(); second++) {
                for (int leg = 0; leg < MATCHES_PER_GROUP_PAIRING; leg++) {
                    MatchOutcome outcome = matchSimulator.playGroupMatch(
                            opponents.get(first).strength(), opponents.get(second).strength(), random);
                    record(standings, first + 1, second + 1, outcome);
                }
            }
        }

        List<GroupStanding> ranking = new ArrayList<>(standings);
        Collections.shuffle(ranking, random);
        ranking.sort(Comparator.comparingInt(GroupStanding::points).reversed()
                .thenComparing(Comparator.comparingInt(GroupStanding::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt(GroupStanding::goalsFor).reversed()));
        int rank = IntStream.range(0, ranking.size())
                .filter(position -> ranking.get(position).player())
                .findFirst()
                .orElseThrow() + 1;
        return new GroupStageResult(playerMatches, ranking, rank);
    }

    private void record(List<GroupStanding> standings, int first, int second, MatchOutcome outcome) {
        standings.set(first, standings.get(first).withMatch(outcome.goalsA(), outcome.goalsB()));
        standings.set(second, standings.get(second).withMatch(outcome.goalsB(), outcome.goalsA()));
    }

    private List<Opponent> sortedByStrength(List<Opponent> opponents) {
        return opponents.stream()
                .sorted(Comparator.comparingDouble((Opponent opponent) -> opponent.strength().overall())
                        .thenComparingInt(opponent -> opponent.teamSeason().teamId())
                        .thenComparingInt(opponent -> opponent.teamSeason().startYear()))
                .toList();
    }

    private record RankWindow(double from, double to) {
        List<Opponent> slice(List<Opponent> sortedOpponents) {
            int size = sortedOpponents.size();
            return sortedOpponents.subList((int) (size * from), (int) (size * to));
        }
    }
}
