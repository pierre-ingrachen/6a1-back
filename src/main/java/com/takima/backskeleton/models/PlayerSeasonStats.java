package com.takima.backskeleton.models;

import java.util.Arrays;
import java.util.Map;

public record PlayerSeasonStats(
        Integer playerId,
        String playerName,
        String position,
        Short heightCm,
        Short weightKg,
        String teamName,
        String eliminationRound,
        Short rating,
        Double averageRating,
        double[] values
) {
    public PlayerSeasonStats(Integer playerId, String playerName, String position, Short heightCm, Short weightKg,
                             String teamName, String eliminationRound, Short rating, Double averageRating,
                             Map<String, Double> valuesByColumn) {
        this(playerId, playerName, position, heightCm, weightKg, teamName, eliminationRound, rating, averageRating,
                toValues(valuesByColumn));
    }

    public static double[] emptyValues() {
        double[] values = new double[Stat.columns().size()];
        Arrays.fill(values, Double.NaN);
        return values;
    }

    public Double value(String column) {
        int index = Stat.columnIndex(column);
        return index < 0 || Double.isNaN(values[index]) ? null : values[index];
    }

    public int minutes() {
        return value(Stat.MINUTES_PLAYED.getColumn()).intValue();
    }

    public int matchesPlayed() {
        return value(Stat.MATCHES_PLAYED.getColumn()).intValue();
    }

    private static double[] toValues(Map<String, Double> valuesByColumn) {
        double[] values = emptyValues();
        valuesByColumn.forEach((column, value) -> {
            int index = Stat.columnIndex(column);
            if (index >= 0 && value != null) {
                values[index] = value;
            }
        });
        return values;
    }
}
