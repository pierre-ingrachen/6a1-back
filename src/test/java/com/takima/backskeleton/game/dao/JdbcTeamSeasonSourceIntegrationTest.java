package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestConfiguration.class, JdbcTeamSeasonSource.class})
class JdbcTeamSeasonSourceIntegrationTest {
    @Autowired
    private JdbcTeamSeasonSource source;

    @Test
    void loadAllGroupsPlayersByTeamSeasonAndKeepsOnlyRatedPlayersWithEnoughMinutes() {
        List<TeamSeason> teamSeasons = source.loadAll();

        assertThat(teamSeasons).extracting(TeamSeason::teamName)
                .containsExactly("Alpha FC", "Beta United", "Gamma Club", "Alpha FC");
        TeamSeason alpha2021 = teamSeasons.get(0);
        assertThat(alpha2021.seasonLabel()).isEqualTo("2021/2022");
        assertThat(alpha2021.roundReached()).isEqualTo(Round.QUARTER_FINAL);
        assertThat(alpha2021.players()).extracting(player -> player.name()).containsExactly("Lionel Messi", "Multi Club");
        assertThat(alpha2021.players().get(0).rating()).isEqualTo(86);
    }

    @Test
    void loadAllMapsWinnersAndGroupStageExits() {
        List<TeamSeason> teamSeasons = source.loadAll();

        assertThat(teamSeasons).filteredOn(teamSeason -> teamSeason.teamName().equals("Gamma Club"))
                .extracting(TeamSeason::roundReached).containsExactly(Round.WINNER);
        assertThat(teamSeasons.get(3).roundReached()).isEqualTo(Round.GROUP_STAGE);
    }
}
