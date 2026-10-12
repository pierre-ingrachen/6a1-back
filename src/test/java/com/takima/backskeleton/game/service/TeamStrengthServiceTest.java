package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Line;
import com.takima.backskeleton.game.model.PickedPlayer;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamStrength;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TeamStrengthServiceTest {
    private final TeamStrengthService service = new TeamStrengthService();

    @Test
    void uniformSquadHasThatRatingEverywhere() {
        TeamStrength strength = service.ofSquad(Formation.FOUR_THREE_THREE, squadOfRating(80));

        assertThat(strength.attack()).isCloseTo(80, within(1e-9));
        assertThat(strength.defense()).isCloseTo(80, within(1e-9));
    }

    @Test
    void starAttackersRaiseAttackMoreThanDefense() {
        Map<Integer, PickedPlayer> picks = new HashMap<>(squadOfRating(70));
        Formation formation = Formation.FOUR_THREE_THREE;
        IntStream.range(0, formation.slots().size())
                .filter(index -> formation.slots().get(index).line() == Line.ATTACKER)
                .forEach(index -> picks.put(index, pick(index, 95)));

        TeamStrength strength = service.ofSquad(formation, picks);

        assertThat(strength.attack()).isGreaterThan(80);
        assertThat(strength.defense()).isLessThan(75);
        assertThat(strength.attack() - strength.defense()).isGreaterThan(10);
    }

    @Test
    void goalkeeperWeighsMoreInDefenseThanASingleDefender() {
        Formation formation = Formation.FOUR_THREE_THREE;
        Map<Integer, PickedPlayer> withStarGoalkeeper = new HashMap<>(squadOfRating(70));
        withStarGoalkeeper.put(0, pick(0, 90));
        Map<Integer, PickedPlayer> withStarDefender = new HashMap<>(squadOfRating(70));
        withStarDefender.put(1, pick(1, 90));

        assertThat(service.ofSquad(formation, withStarGoalkeeper).defense())
                .isGreaterThan(service.ofSquad(formation, withStarDefender).defense());
    }

    @Test
    void opponentStrengthUsesTheBestPlayersOfEachLine() {
        TeamSeason team = teamSeason(List.of(
                player(1, 90, Line.GOALKEEPER, 900),
                player(2, 50, Line.GOALKEEPER, 900),
                player(3, 80, Line.DEFENDER, 900), player(4, 80, Line.DEFENDER, 900),
                player(5, 80, Line.DEFENDER, 900), player(6, 80, Line.DEFENDER, 900),
                player(7, 40, Line.DEFENDER, 900),
                player(8, 80, Line.MIDFIELDER, 900), player(9, 80, Line.MIDFIELDER, 900), player(10, 80, Line.MIDFIELDER, 900),
                player(11, 80, Line.ATTACKER, 900), player(12, 80, Line.ATTACKER, 900), player(13, 80, Line.ATTACKER, 900)));

        TeamStrength strength = service.ofOpponent(team);

        assertThat(strength.attack()).isCloseTo(80, within(1e-9));
        assertThat(strength.defense()).isGreaterThan(80);
    }

    @Test
    void opponentIgnoresPlayersWithTooFewMinutes() {
        TeamSeason team = teamSeason(List.of(
                player(1, 90, Line.GOALKEEPER, GameBalance.MIN_OPPONENT_MINUTES - 1),
                player(2, 70, Line.GOALKEEPER, 900)));

        TeamStrength withBench = service.ofOpponent(team);
        TeamStrength withoutBench = service.ofOpponent(teamSeason(List.of(player(2, 70, Line.GOALKEEPER, 900))));

        assertThat(withBench).isEqualTo(withoutBench);
    }

    @Test
    void missingPlayersAreReplacedByTheDefaultRating() {
        TeamStrength strength = service.ofOpponent(teamSeason(List.of()));

        assertThat(strength.attack()).isCloseTo(GameBalance.MISSING_PLAYER_RATING, within(1e-9));
        assertThat(strength.defense()).isCloseTo(GameBalance.MISSING_PLAYER_RATING, within(1e-9));
    }

    private Map<Integer, PickedPlayer> squadOfRating(int rating) {
        return IntStream.range(0, Formation.SQUAD_SIZE).boxed()
                .collect(Collectors.toMap(index -> index, index -> pick(index, rating)));
    }

    private PickedPlayer pick(int id, int rating) {
        return new PickedPlayer(id, "Player " + id, rating, "Team", "2020/2021", Set.of());
    }

    private SeasonPlayer player(int id, int rating, Line line, int minutes) {
        return new SeasonPlayer(id, "Player " + id, rating, line, Set.of(), minutes);
    }

    private TeamSeason teamSeason(List<SeasonPlayer> players) {
        return new TeamSeason(1, "Team", "Country", 2020, "2020/2021", Round.ROUND_OF_16, players);
    }
}
