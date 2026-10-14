package com.takima.backskeleton.game.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OpponentTest {

    @Test
    void summaryExposesMostUsedPlayersFirst() {
        List<SeasonPlayer> players = IntStream.range(0, 20)
                .mapToObj(index -> new SeasonPlayer(
                        index, "Player " + index, 70 + index, Line.ATTACKER, Set.of(FieldPosition.ST), index * 100))
                .toList();
        TeamSeason teamSeason = new TeamSeason(1, "Team", "Country", 2020, "2020/2021", Round.ROUND_OF_16, players);
        Opponent opponent = new Opponent(teamSeason, new TeamStrength(80, 80));

        List<OpponentPlayer> summary = opponent.summary().players();

        assertThat(summary).hasSize(14);
        assertThat(summary.get(0)).isEqualTo(new OpponentPlayer("Player 19", 89, Line.ATTACKER));
    }
}
