package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerSeasonTeamDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.DTO.StatDto;
import com.takima.backskeleton.DTO.StatValueDto;
import com.takima.backskeleton.exceptions.PlayerStatsNotFoundException;
import com.takima.backskeleton.models.PlayerAppearance;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {
    private static final Short SEASON = 2025;
    private static final int TARGET_ID = 1;

    @Mock
    private PlayerSeasonStatsDao playerSeasonStatsDao;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void findPlayerStatsKeepsTeamEliminatedInGroupStageAsFirstTeam() {
        givenSeasonRows(
                stint(TARGET_ID, "Late Club", "Quarts de finale", 7, 630, 5),
                stint(TARGET_ID, "Early Club", "Phase de groupes", 4, 360, 1)
        );

        PlayerStatsDto stats = playerService.findPlayerStats(TARGET_ID, SEASON);

        assertThat(stats.teamName()).isEqualTo("Early Club");
        assertThat(stats.teamEstimated()).isFalse();
        assertThat(total(stats, Stat.GOALS_SCORED).value()).isEqualTo(1.0);
    }

    @Test
    void findPlayerStatsFallsBackToTeamWithMostMatchesAndFlagsEstimation() {
        givenSeasonRows(
                stint(TARGET_ID, "Semi Finalist", "Demi-finales", 5, 400, 3),
                stint(TARGET_ID, "Round Of 16", "Huitièmes de finale", 2, 180, 1)
        );

        PlayerStatsDto stats = playerService.findPlayerStats(TARGET_ID, SEASON);

        assertThat(stats.teamName()).isEqualTo("Semi Finalist");
        assertThat(stats.teamEstimated()).isTrue();
        assertThat(stats.minutes()).isEqualTo(400);
        assertThat(total(stats, Stat.GOALS_SCORED).value()).isEqualTo(3.0);
    }

    @Test
    void findPlayerStatsComputesPer90ValuesOnlyForCountingStats() {
        givenSeasonRows(player(TARGET_ID, "Forward", 540, "buts", 6));

        PlayerStatsDto stats = playerService.findPlayerStats(TARGET_ID, SEASON);

        assertThat(total(stats, Stat.GOALS_SCORED).value()).isEqualTo(6.0);
        assertThat(per90(stats, Stat.GOALS_SCORED).value()).isEqualTo(1.0);
        assertThat(per90(stats, Stat.MINUTES_PLAYED).value()).isEqualTo(540.0);
    }

    @Test
    void findPlayerStatsRanksAgainstQualifiedPlayers() {
        givenSeasonRows(
                player(TARGET_ID, "Forward", 900, "buts", 5),
                player(2, "Forward", 900, "buts", 2),
                player(3, "Forward", 900, "buts", 4),
                player(4, "Forward", 900, "buts", 6),
                player(5, "Forward", 900, "buts", 8)
        );

        StatValueDto goals = total(playerService.findPlayerStats(TARGET_ID, SEASON), Stat.GOALS_SCORED);

        assertThat(goals.percentile()).isEqualTo(50);
        assertThat(goals.topPercent()).isEqualTo(50);
    }

    @Test
    void findPlayerStatsInvertsPercentileWhenLowerIsBetter() {
        givenSeasonRows(
                player(TARGET_ID, "Defender", 900, "cartons_jaunes", 1),
                player(2, "Defender", 900, "cartons_jaunes", 0),
                player(3, "Defender", 900, "cartons_jaunes", 2),
                player(4, "Defender", 900, "cartons_jaunes", 3),
                player(5, "Defender", 900, "cartons_jaunes", 4)
        );

        PlayerStatsDto stats = playerService.findPlayerStats(TARGET_ID, SEASON);

        assertThat(total(stats, Stat.YELLOW_CARDS).percentile()).isEqualTo(75);
        assertThat(total(stats, Stat.YELLOW_CARDS).topPercent()).isEqualTo(25);
        assertThat(stat(stats, Stat.YELLOW_CARDS).lowerIsBetter()).isTrue();
    }

    @Test
    void findPlayerStatsCountsTiesAsHalfBetter() {
        givenSeasonRows(
                player(TARGET_ID, "Defender", 900, "cartons_rouges", 0),
                player(2, "Defender", 900, "cartons_rouges", 0),
                player(3, "Defender", 900, "cartons_rouges", 0),
                player(4, "Defender", 900, "cartons_rouges", 0),
                player(5, "Defender", 900, "cartons_rouges", 1)
        );

        StatValueDto redCards = total(playerService.findPlayerStats(TARGET_ID, SEASON), Stat.RED_CARDS);

        assertThat(redCards.percentile()).isEqualTo(63);
    }

    @Test
    void findPlayerStatsExcludesPlayersBelowMinutesThresholdFromPopulation() {
        int belowThreshold = PlayerService.MINIMUM_MINUTES_FOR_PERCENTILES - 1;
        givenSeasonRows(
                player(TARGET_ID, "Forward", 100, "buts", 5),
                player(2, "Forward", 900, "buts", 2),
                player(3, "Forward", 900, "buts", 4),
                player(4, "Forward", 900, "buts", 6),
                player(5, "Forward", 900, "buts", 8),
                player(6, "Forward", belowThreshold, "buts", 100)
        );

        StatValueDto goals = total(playerService.findPlayerStats(TARGET_ID, SEASON), Stat.GOALS_SCORED);

        assertThat(goals.percentile()).isEqualTo(50);
    }

    @Test
    void findPlayerStatsHidesRatioBelowMinimumAttemptsAndExcludesItFromPopulation() {
        givenSeasonRows(
                player(TARGET_ID, "Forward", 900, "buts", 4, "tirs", 10),
                player(2, "Forward", 900, "buts", 6, "tirs", 20),
                player(3, "Forward", 900, "buts", 5, "tirs", 5),
                player(4, "Forward", 900, "buts", 3, "tirs", 9)
        );

        StatValueDto conversion = total(playerService.findPlayerStats(TARGET_ID, SEASON), Stat.CONVERSION_RATE);
        StatValueDto hiddenConversion = total(playerService.findPlayerStats(4, SEASON), Stat.CONVERSION_RATE);

        assertThat(conversion.value()).isEqualTo(40.0);
        assertThat(conversion.percentile()).isEqualTo(100);
        assertThat(hiddenConversion).isEqualTo(new StatValueDto(null, null, null));
    }

    @Test
    void findPlayerStatsRanksGoalkeeperStatsAmongGoalkeepersOnly() {
        givenSeasonRows(
                player(TARGET_ID, "Goalkeeper", 900, "arrets", 20),
                player(2, "Goalkeeper", 900, "arrets", 30),
                player(3, "Defender", 900, "arrets", 0),
                player(4, "Defender", 900, "arrets", 0)
        );

        StatValueDto goalkeeperSaves = total(playerService.findPlayerStats(TARGET_ID, SEASON), Stat.SAVES);
        StatValueDto outfieldSaves = total(playerService.findPlayerStats(3, SEASON), Stat.SAVES);

        assertThat(goalkeeperSaves.percentile()).isZero();
        assertThat(outfieldSaves.value()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "Forward, GOALS, SHOTS",
            "Midfielder, CREATION, PASSING",
            "Defender, DEFENDING, AERIAL_DUELS",
            "Goalkeeper, GOALKEEPING, PASSING"
    })
    void findPlayerStatsOrdersCategoriesByPosition(String position, String firstCategory, String secondCategory) {
        givenSeasonRows(player(TARGET_ID, position, 900));

        List<String> categoryKeys = playerService.findPlayerStats(TARGET_ID, SEASON).categories().stream()
                .map(category -> category.key())
                .toList();

        assertThat(categoryKeys).startsWith(firstCategory, secondCategory).hasSize(11);
    }

    @Test
    void findPlayerStatsThrowsWhenPlayerHasNoStatsInSeason() {
        givenSeasonRows(player(2, "Forward", 900));

        assertThatThrownBy(() -> playerService.findPlayerStats(TARGET_ID, SEASON))
                .isInstanceOf(PlayerStatsNotFoundException.class);
    }

    @Test
    void searchPlayersIgnoresAccentsAndCaseAndSortsByName() {
        givenAppearances(
                appearance(1, "Kylian Mbappé", 2025, "Real Madrid"),
                appearance(2, "Bryan Mbeumo", 2025, "Manchester United"),
                appearance(3, "Erling Haaland", 2025, "Manchester City")
        );

        assertThat(playerService.searchPlayers(null, "MBAPPE"))
                .extracting(PlayerSearchResultDto::name)
                .containsExactly("Kylian Mbappé");
        assertThat(playerService.searchPlayers(null, "mb"))
                .extracting(PlayerSearchResultDto::name)
                .containsExactly("Bryan Mbeumo", "Kylian Mbappé");
    }

    @Test
    void searchPlayersFindsRetiredPlayersWithAllTheirSeasonsAndLatestTeam() {
        givenAppearances(
                appearance(7, "Cristiano Ronaldo", 2021, "Manchester United"),
                appearance(7, "Cristiano Ronaldo", 2018, "Juventus"),
                appearance(7, "Cristiano Ronaldo", 2018, "Real Madrid"),
                appearance(8, "Erling Haaland", 2025, "Manchester City")
        );

        List<PlayerSearchResultDto> results = playerService.searchPlayers(null, "ronaldo");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).teamName()).isEqualTo("Manchester United");
        assertThat(results.get(0).seasons()).containsExactly(
                new PlayerSeasonTeamDto((short) 2021, "Manchester United"),
                new PlayerSeasonTeamDto((short) 2018, "Juventus"));
    }

    @Test
    void searchPlayersWithSeasonKeepsOnlyPlayersOfThatSeasonWithTheirTeamThatSeason() {
        givenAppearances(
                appearance(7, "Cristiano Ronaldo", 2021, "Manchester United"),
                appearance(7, "Cristiano Ronaldo", 2018, "Juventus"),
                appearance(9, "Ronaldo Nazario", 2021, "Real Valladolid"),
                appearance(10, "Ronaldo Retired", 2015, "Old Club")
        );

        List<PlayerSearchResultDto> results = playerService.searchPlayers((short) 2018, "ronaldo");

        assertThat(results).extracting(PlayerSearchResultDto::name).containsExactly("Cristiano Ronaldo");
        assertThat(results.get(0).teamName()).isEqualTo("Juventus");
        assertThat(results.get(0).seasons()).containsExactly(
                new PlayerSeasonTeamDto((short) 2021, "Manchester United"),
                new PlayerSeasonTeamDto((short) 2018, "Juventus"));
    }

    @Test
    void searchPlayersReturnsAtMostTwentyResults() {
        givenAppearances(IntStream.rangeClosed(1, 30)
                .mapToObj(id -> appearance(id, "Player " + id, 2025, "Team"))
                .toArray(PlayerAppearance[]::new));

        assertThat(playerService.searchPlayers(null, "player")).hasSize(PlayerService.MAXIMUM_SEARCH_RESULTS);
    }

    @Test
    void searchPlayersReturnsNothingForTooShortQuery() {
        assertThat(playerService.searchPlayers(null, " m ")).isEmpty();
        verify(playerSeasonStatsDao, never()).findAllAppearances();
    }

    private void givenAppearances(PlayerAppearance... appearances) {
        when(playerSeasonStatsDao.findAllAppearances()).thenReturn(List.of(appearances));
    }

    private static PlayerAppearance appearance(int id, String name, int season, String team) {
        return new PlayerAppearance(id, name, "Forward", (short) season, team);
    }

    private void givenSeasonRows(PlayerSeasonStats... rows) {
        when(playerSeasonStatsDao.findBySeason(SEASON)).thenReturn(List.of(rows));
    }

    private static PlayerSeasonStats player(int id, String position, int minutes, Object... columnValues) {
        Map<String, Double> values = new HashMap<>(Map.of("minutes", (double) minutes, "matchs_joues", 6.0));
        IntStream.range(0, columnValues.length / 2)
                .forEach(index -> values.put((String) columnValues[2 * index], ((Number) columnValues[2 * index + 1]).doubleValue()));
        return new PlayerSeasonStats(id, "Player " + id, position, null, null, "Team " + id, null, null, null, values);
    }

    private static PlayerSeasonStats named(int id, String name) {
        PlayerSeasonStats stats = player(id, "Forward", 900);
        return new PlayerSeasonStats(id, name, stats.position(), null, null, stats.teamName(), null, null, null, stats.values());
    }

    private static PlayerSeasonStats stint(int id, String teamName, String eliminationRound, int matches, int minutes, int goals) {
        Map<String, Double> values = Map.of("minutes", (double) minutes, "matchs_joues", (double) matches, "buts", (double) goals);
        return new PlayerSeasonStats(id, "Player " + id, "Forward", null, null, teamName, eliminationRound, null, null, values);
    }

    private static StatDto stat(PlayerStatsDto stats, Stat stat) {
        return stats.categories().stream()
                .flatMap(category -> category.stats().stream())
                .filter(candidate -> candidate.key().equals(stat.name()))
                .findFirst()
                .orElseThrow();
    }

    private static StatValueDto total(PlayerStatsDto stats, Stat stat) {
        return stat(stats, stat).total();
    }

    private static StatValueDto per90(PlayerStatsDto stats, Stat stat) {
        return stat(stats, stat).per90();
    }
}
