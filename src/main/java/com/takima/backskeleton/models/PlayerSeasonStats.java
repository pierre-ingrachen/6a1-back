package com.takima.backskeleton.models;

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
        Map<String, Double> values
) {
    public Double value(String column) {
        return values.get(column);
    }

    public int minutes() {
        return value(Stat.MINUTES_PLAYED.getColumn()).intValue();
    }

    public int matchesPlayed() {
        return value(Stat.MATCHES_PLAYED.getColumn()).intValue();
    }
}
