package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Line;
import com.takima.backskeleton.game.model.PickedPlayer;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamStrength;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class TeamStrengthService {
    private static final Map<Line, Integer> OPPONENT_LINEUP = Map.of(
            Line.GOALKEEPER, 1,
            Line.DEFENDER, 4,
            Line.MIDFIELDER, 3,
            Line.ATTACKER, 3);

    public TeamStrength ofSquad(Formation formation, Map<Integer, PickedPlayer> picks) {
        Map<Line, List<Integer>> ratingsByLine = picks.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> formation.slots().get(entry.getKey()).line(),
                        () -> new EnumMap<>(Line.class),
                        Collectors.mapping(entry -> entry.getValue().rating(), Collectors.toList())));
        return fromRatings(ratingsByLine);
    }

    public TeamStrength ofOpponent(TeamSeason teamSeason) {
        Map<Line, List<Integer>> ratingsByLine = OPPONENT_LINEUP.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> bestRatings(teamSeason, entry.getKey(), entry.getValue()),
                        (first, second) -> first,
                        () -> new EnumMap<>(Line.class)));
        return fromRatings(ratingsByLine);
    }

    private List<Integer> bestRatings(TeamSeason teamSeason, Line line, int count) {
        List<Integer> best = teamSeason.players().stream()
                .filter(player -> player.minutes() >= GameBalance.MIN_OPPONENT_MINUTES)
                .filter(player -> player.ratingLine() == line)
                .map(SeasonPlayer::rating)
                .sorted(Comparator.reverseOrder())
                .limit(count)
                .toList();
        return Stream.concat(best.stream(), Stream.generate(() -> GameBalance.MISSING_PLAYER_RATING))
                .limit(count)
                .toList();
    }

    TeamStrength fromRatings(Map<Line, List<Integer>> ratingsByLine) {
        double goalkeeper = average(ratingsByLine, Line.GOALKEEPER);
        double defenders = average(ratingsByLine, Line.DEFENDER);
        double midfielders = average(ratingsByLine, Line.MIDFIELDER);
        double attackers = average(ratingsByLine, Line.ATTACKER);
        int defenderCount = count(ratingsByLine, Line.DEFENDER);
        int midfielderCount = count(ratingsByLine, Line.MIDFIELDER);
        int attackerCount = count(ratingsByLine, Line.ATTACKER);

        double midfieldWeight = midfielderCount * GameBalance.MIDFIELD_SHARE;
        double attack = (attackers * attackerCount + midfielders * midfieldWeight) / (attackerCount + midfieldWeight);
        double defense = (defenders * defenderCount + midfielders * midfieldWeight + goalkeeper * GameBalance.GOALKEEPER_WEIGHT)
                / (defenderCount + midfieldWeight + GameBalance.GOALKEEPER_WEIGHT);
        return new TeamStrength(attack, defense);
    }

    private double average(Map<Line, List<Integer>> ratingsByLine, Line line) {
        return ratingsByLine.getOrDefault(line, List.of()).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(GameBalance.MISSING_PLAYER_RATING);
    }

    private int count(Map<Line, List<Integer>> ratingsByLine, Line line) {
        return ratingsByLine.getOrDefault(line, List.of()).size();
    }
}
