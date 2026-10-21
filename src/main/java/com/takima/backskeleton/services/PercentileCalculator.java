package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.StatValueDto;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import com.takima.backskeleton.models.StatCategory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PercentileCalculator {
    public static final int MINIMUM_MINUTES_FOR_PERCENTILES = 450;
    public static final StatValueDto NO_VALUE = new StatValueDto(null, null, null);

    private static final String GOALKEEPER = "Goalkeeper";

    public PercentileDistribution distributionOf(Collection<PlayerSeasonStats> seasonPlayers) {
        List<PlayerSeasonStats> qualified = seasonPlayers.stream().filter(this::isQualified).toList();
        List<PlayerSeasonStats> qualifiedGoalkeepers = qualified.stream().filter(this::isGoalkeeper).toList();
        return new PercentileDistribution(
                sortedValues(qualified, qualifiedGoalkeepers, this::totalValue),
                sortedValues(qualified, qualifiedGoalkeepers, this::per90Value));
    }

    public StatValueDto total(Stat stat, PlayerSeasonStats player, PercentileDistribution distribution) {
        return rank(stat, player, distribution.totals().get(stat), totalValue(stat, player));
    }

    public StatValueDto per90(Stat stat, PlayerSeasonStats player, PercentileDistribution distribution) {
        if (!stat.isPer90Applicable()) {
            return total(stat, player, distribution);
        }
        return rank(stat, player, distribution.per90().get(stat), per90Value(stat, player));
    }

    public double totalValue(Stat stat, PlayerSeasonStats player) {
        if (!stat.isRatio()) {
            return valueOf(player, stat.getColumn());
        }
        double attempts = valueOf(player, stat.getDenominatorColumn());
        double successes = valueOf(player, stat.getNumeratorColumn());
        if (Double.isNaN(attempts) || Double.isNaN(successes) || attempts < stat.getMinimumAttempts()) {
            return Double.NaN;
        }
        return 100.0 * successes / attempts;
    }

    public double per90Value(Stat stat, PlayerSeasonStats player) {
        double total = totalValue(stat, player);
        if (Double.isNaN(total) || player.minutes() == 0) {
            return Double.NaN;
        }
        return total * 90 / player.minutes();
    }

    private Map<Stat, double[]> sortedValues(List<PlayerSeasonStats> qualified, List<PlayerSeasonStats> qualifiedGoalkeepers,
                                             ToDoubleBiFunction<Stat, PlayerSeasonStats> valueOf) {
        return Arrays.stream(Stat.values())
                .collect(Collectors.toUnmodifiableMap(stat -> stat, stat -> sortedValuesOf(
                        stat, stat.getCategory() == StatCategory.GOALKEEPING ? qualifiedGoalkeepers : qualified, valueOf)));
    }

    private double[] sortedValuesOf(Stat stat, List<PlayerSeasonStats> population, ToDoubleBiFunction<Stat, PlayerSeasonStats> valueOf) {
        return population.stream()
                .mapToDouble(player -> valueOf.applyAsDouble(stat, player))
                .filter(value -> !Double.isNaN(value))
                .sorted()
                .toArray();
    }

    private StatValueDto rank(Stat stat, PlayerSeasonStats player, double[] sortedValues, double value) {
        if (Double.isNaN(value)) {
            return NO_VALUE;
        }
        if (stat.getCategory() == StatCategory.GOALKEEPING && !isGoalkeeper(player)) {
            return NO_VALUE;
        }
        int selfCount = isQualified(player) ? 1 : 0;
        int populationSize = sortedValues.length - selfCount;
        if (populationSize == 0) {
            return new StatValueDto(value, null, null);
        }
        int firstEqual = lowerBound(sortedValues, value);
        int firstGreater = upperBound(sortedValues, value);
        int tiedCount = firstGreater - firstEqual - selfCount;
        int worseCount = stat.isLowerBetter() ? sortedValues.length - firstGreater : firstEqual;
        int percentile = (int) Math.round(100.0 * (worseCount + tiedCount / 2.0) / populationSize);
        return new StatValueDto(value, percentile, Math.max(1, 100 - percentile));
    }

    private int lowerBound(double[] sortedValues, double value) {
        int low = 0;
        int high = sortedValues.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sortedValues[middle] < value) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private int upperBound(double[] sortedValues, double value) {
        int low = 0;
        int high = sortedValues.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sortedValues[middle] <= value) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private double valueOf(PlayerSeasonStats player, String column) {
        Double value = player.value(column);
        return value == null ? Double.NaN : value + 0.0;
    }

    private boolean isQualified(PlayerSeasonStats player) {
        return player.minutes() >= MINIMUM_MINUTES_FOR_PERCENTILES;
    }

    private boolean isGoalkeeper(PlayerSeasonStats player) {
        return GOALKEEPER.equals(player.position());
    }

    @FunctionalInterface
    private interface ToDoubleBiFunction<A, B> {
        double applyAsDouble(A first, B second);
    }
}
