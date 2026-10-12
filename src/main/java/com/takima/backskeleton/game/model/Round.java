package com.takima.backskeleton.game.model;

import java.util.List;

public enum Round {
    GROUP_STAGE,
    ROUND_OF_16,
    QUARTER_FINAL,
    SEMI_FINAL,
    FINAL,
    WINNER;

    public static final List<Round> KNOCKOUT_ROUNDS = List.of(ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL);

    public static Round fromDatabase(String lastRoundPlayed, boolean winner) {
        if (winner) {
            return WINNER;
        }
        return switch (lastRoundPlayed.trim()) {
            case "Phase de groupes", "Phase de ligue", "Barrages" -> GROUP_STAGE;
            case "Huitièmes de finale" -> ROUND_OF_16;
            case "Quarts de finale" -> QUARTER_FINAL;
            case "Demi-finales" -> SEMI_FINAL;
            case "Finale" -> FINAL;
            default -> throw new IllegalArgumentException("Unknown round " + lastRoundPlayed);
        };
    }

    public boolean hasPlayed(Round round) {
        return compareTo(round) >= 0;
    }
}
