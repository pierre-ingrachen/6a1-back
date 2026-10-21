package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.StatValueDto;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

class PercentileCalculatorTest {
    private static final String[] POSITIONS = {"Forward", "Midfielder", "Defender", "Goalkeeper"};

    private final PercentileCalculator calculator = new PercentileCalculator();

    @Test
    void matchesTheReferenceAlgorithmOnVariedSeasons() {
        for (long seed = 1; seed <= 25; seed++) {
            List<PlayerSeasonStats> season = randomSeason(new Random(seed), 120);
            PercentileDistribution distribution = calculator.distributionOf(season);

            for (PlayerSeasonStats player : season) {
                for (Stat stat : Stat.values()) {
                    assertThat(calculator.total(stat, player, distribution))
                            .as("seed %d player %d total %s", seed, player.playerId(), stat)
                            .isEqualTo(referenceTotal(stat, player, season));
                    assertThat(calculator.per90(stat, player, distribution))
                            .as("seed %d player %d per90 %s", seed, player.playerId(), stat)
                            .isEqualTo(referencePer90(stat, player, season));
                }
            }
        }
    }

    @Test
    void ranksLowerIsBetterStatsInReverse() {
        List<PlayerSeasonStats> season = List.of(
                player(1, "Defender", 900, "cartons_jaunes", 1),
                player(2, "Defender", 900, "cartons_jaunes", 0),
                player(3, "Defender", 900, "cartons_jaunes", 2),
                player(4, "Defender", 900, "cartons_jaunes", 3),
                player(5, "Defender", 900, "cartons_jaunes", 4));

        StatValueDto yellowCards = calculator.total(Stat.YELLOW_CARDS, season.get(0), calculator.distributionOf(season));

        assertThat(yellowCards.percentile()).isEqualTo(75);
        assertThat(yellowCards.topPercent()).isEqualTo(25);
    }

    @Test
    void countsTiesAsHalfBetter() {
        List<PlayerSeasonStats> season = List.of(
                player(1, "Defender", 900, "cartons_rouges", 0),
                player(2, "Defender", 900, "cartons_rouges", 0),
                player(3, "Defender", 900, "cartons_rouges", 0),
                player(4, "Defender", 900, "cartons_rouges", 0),
                player(5, "Defender", 900, "cartons_rouges", 1));

        assertThat(calculator.total(Stat.RED_CARDS, season.get(0), calculator.distributionOf(season)).percentile()).isEqualTo(63);
    }

    @Test
    void excludesThePlayerHimselfFromHisPopulationOnlyWhenQualified() {
        PlayerSeasonStats unqualified = player(1, "Forward", 100, "buts", 5);
        List<PlayerSeasonStats> season = List.of(
                unqualified,
                player(2, "Forward", 900, "buts", 2),
                player(3, "Forward", 900, "buts", 4),
                player(4, "Forward", 900, "buts", 6),
                player(5, "Forward", 900, "buts", 8));

        assertThat(calculator.total(Stat.GOALS_SCORED, unqualified, calculator.distributionOf(season)).percentile()).isEqualTo(50);
    }

    @Test
    void returnsOnlyTheValueWhenNobodyElseIsQualified() {
        PlayerSeasonStats alone = player(1, "Forward", 900, "buts", 5);

        StatValueDto goals = calculator.total(Stat.GOALS_SCORED, alone, calculator.distributionOf(List.of(alone)));

        assertThat(goals).isEqualTo(new StatValueDto(5.0, null, null));
    }

    @Test
    void hidesRatiosBelowMinimumAttempts() {
        PlayerSeasonStats fewShots = player(1, "Forward", 900, "buts", 3, "tirs", 9);

        StatValueDto conversion = calculator.total(Stat.CONVERSION_RATE, fewShots, calculator.distributionOf(List.of(fewShots)));

        assertThat(conversion).isEqualTo(PercentileCalculator.NO_VALUE);
    }

    @Test
    void ranksGoalkeeperStatsAmongGoalkeepersOnly() {
        List<PlayerSeasonStats> season = List.of(
                player(1, "Goalkeeper", 900, "arrets", 20),
                player(2, "Goalkeeper", 900, "arrets", 30),
                player(3, "Defender", 900, "arrets", 0));
        PercentileDistribution distribution = calculator.distributionOf(season);

        assertThat(calculator.total(Stat.SAVES, season.get(0), distribution).percentile()).isZero();
        assertThat(calculator.total(Stat.SAVES, season.get(2), distribution)).isEqualTo(PercentileCalculator.NO_VALUE);
    }

    private List<PlayerSeasonStats> randomSeason(Random random, int size) {
        List<PlayerSeasonStats> season = new ArrayList<>();
        for (int id = 1; id <= size; id++) {
            Map<String, Double> values = new HashMap<>();
            values.put("minutes", (double) (random.nextInt(4) == 0 ? random.nextInt(450) : 450 + random.nextInt(3000)));
            values.put("matchs_joues", (double) (1 + random.nextInt(12)));
            Stat.columns().stream()
                    .filter(column -> !values.containsKey(column))
                    .filter(column -> random.nextInt(10) != 0)
                    .forEach(column -> values.put(column, (double) random.nextInt(random.nextBoolean() ? 4 : 60)));
            season.add(new PlayerSeasonStats(id, "Player " + id, POSITIONS[random.nextInt(POSITIONS.length)],
                    null, null, "Team", null, null, null, values));
        }
        return season;
    }

    private static PlayerSeasonStats player(int id, String position, int minutes, Object... columnValues) {
        Map<String, Double> values = new HashMap<>(Map.of("minutes", (double) minutes, "matchs_joues", 6.0));
        for (int index = 0; index < columnValues.length; index += 2) {
            values.put((String) columnValues[index], ((Number) columnValues[index + 1]).doubleValue());
        }
        return new PlayerSeasonStats(id, "Player " + id, position, null, null, "Team", null, null, null, values);
    }

    private StatValueDto referenceTotal(Stat stat, PlayerSeasonStats player, List<PlayerSeasonStats> season) {
        return referenceStatValue(stat, player, season, this::referenceTotalValue);
    }

    private StatValueDto referencePer90(Stat stat, PlayerSeasonStats player, List<PlayerSeasonStats> season) {
        return stat.isPer90Applicable()
                ? referenceStatValue(stat, player, season, this::referencePer90Value)
                : referenceTotal(stat, player, season);
    }

    private StatValueDto referenceStatValue(Stat stat, PlayerSeasonStats player, List<PlayerSeasonStats> season,
                                            BiFunction<Stat, PlayerSeasonStats, Double> valueOf) {
        boolean goalkeeping = stat.getCategory().name().equals("GOALKEEPING");
        if (goalkeeping && !"Goalkeeper".equals(player.position())) {
            return new StatValueDto(null, null, null);
        }
        List<PlayerSeasonStats> population = season.stream()
                .filter(other -> other.minutes() >= PercentileCalculator.MINIMUM_MINUTES_FOR_PERCENTILES)
                .filter(other -> !other.playerId().equals(player.playerId()))
                .filter(other -> !goalkeeping || "Goalkeeper".equals(other.position()))
                .toList();
        Double value = valueOf.apply(stat, player);
        if (value == null) {
            return new StatValueDto(null, null, null);
        }
        List<Double> otherValues = population.stream().map(other -> valueOf.apply(stat, other)).filter(Objects::nonNull).toList();
        if (otherValues.isEmpty()) {
            return new StatValueDto(value, null, null);
        }
        long worse = otherValues.stream().filter(other -> stat.isLowerBetter() ? other > value : other < value).count();
        long tied = otherValues.stream().filter(other -> Double.compare(other, value) == 0).count();
        int percentile = (int) Math.round(100.0 * (worse + tied / 2.0) / otherValues.size());
        return new StatValueDto(value, percentile, Math.max(1, 100 - percentile));
    }

    private Double referenceTotalValue(Stat stat, PlayerSeasonStats player) {
        if (!stat.isRatio()) {
            return player.value(stat.getColumn());
        }
        Double attempts = player.value(stat.getDenominatorColumn());
        Double successes = player.value(stat.getNumeratorColumn());
        if (attempts == null || successes == null || attempts < stat.getMinimumAttempts()) {
            return null;
        }
        return 100.0 * successes / attempts;
    }

    private Double referencePer90Value(Stat stat, PlayerSeasonStats player) {
        Double total = referenceTotalValue(stat, player);
        if (total == null || player.minutes() == 0) {
            return null;
        }
        return total * 90 / player.minutes();
    }
}
