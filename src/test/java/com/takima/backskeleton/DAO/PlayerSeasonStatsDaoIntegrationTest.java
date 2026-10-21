package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.PlayerAppearance;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestConfiguration.class, PlayerSeasonStatsDao.class})
class PlayerSeasonStatsDaoIntegrationTest {
    private static final short SEASON = 2021;
    private static final int MESSI_ID = 11119;

    @Autowired
    private PlayerSeasonStatsDao dao;

    @Test
    void findBySeasonReadsTheRatingOfEachPlayer() {
        PlayerSeasonStats messi = statsOf(MESSI_ID);

        assertThat(messi.rating()).isEqualTo((short) 86);
        assertThat(messi.averageRating()).isEqualTo(7.85);
    }

    @Test
    void findBySeasonReadsIdentityTeamAndEliminationRound() {
        PlayerSeasonStats messi = statsOf(MESSI_ID);

        assertThat(messi.playerName()).isEqualTo("Lionel Messi");
        assertThat(messi.position()).isEqualTo("Forward");
        assertThat(messi.heightCm()).isEqualTo((short) 170);
        assertThat(messi.weightKg()).isEqualTo((short) 72);
        assertThat(messi.teamName()).isEqualTo("Alpha FC");
        assertThat(messi.eliminationRound()).isEqualTo("Quarts de finale");
    }

    @Test
    void findBySeasonKeepsUnknownMeasuresAsNull() {
        PlayerSeasonStats goalkeeper = statsOf(3);

        assertThat(goalkeeper.heightCm()).isNull();
        assertThat(goalkeeper.weightKg()).isNull();
    }

    @Test
    void findBySeasonComputesDerivedStats() {
        PlayerSeasonStats messi = statsOf(MESSI_ID);

        assertThat(messi.value(Stat.STARTS.getColumn())).isEqualTo(6.0);
        assertThat(messi.value(Stat.SUCCESSFUL_PASSES.getColumn())).isEqualTo(120.0);
    }

    @Test
    void findBySeasonReadsEveryStatColumn() {
        PlayerSeasonStats messi = statsOf(MESSI_ID);

        assertThat(messi.value(Stat.GOALS_SCORED.getColumn())).isEqualTo(5.0);
        assertThat(messi.value(Stat.MINUTES_PLAYED.getColumn())).isEqualTo(640.0);
        assertThat(messi.value(Stat.MATCHES_PLAYED.getColumn())).isEqualTo(8.0);
        assertThat(messi.value(Stat.SHOTS_TAKEN.getColumn())).isEqualTo(20.0);
    }

    @Test
    void findBySeasonReturnsOneRowPerTeamOfTheSeason() {
        List<PlayerSeasonStats> multiClub = dao.findBySeason(SEASON).stream()
                .filter(stats -> stats.playerId() == 4)
                .toList();

        assertThat(multiClub).extracting(PlayerSeasonStats::teamName).containsExactlyInAnyOrder("Alpha FC", "Beta United");
    }

    @Test
    void findBySeasonIgnoresOtherSeasons() {
        assertThat(dao.findBySeason((short) 2022)).extracting(PlayerSeasonStats::playerId).containsExactly(MESSI_ID);
        assertThat(dao.findBySeason((short) 1999)).isEmpty();
    }

    @Test
    void findAllAppearancesListsEveryPlayerSeasonWithTeam() {
        List<PlayerAppearance> appearances = dao.findAllAppearances();

        assertThat(appearances).hasSize(6);
        assertThat(appearances).contains(new PlayerAppearance(MESSI_ID, "Lionel Messi", "Forward", (short) 2021, "Alpha FC"));
        assertThat(appearances.get(0).season()).isEqualTo((short) 2022);
    }

    private PlayerSeasonStats statsOf(int playerId) {
        return dao.findBySeason(SEASON).stream()
                .filter(stats -> stats.playerId() == playerId)
                .findFirst()
                .orElseThrow();
    }
}
