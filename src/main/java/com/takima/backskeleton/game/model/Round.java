package com.takima.backskeleton.game.model;

import java.util.List;
import java.util.Set;

public enum Round {
    GROUP_STAGE,
    ROUND_OF_16,
    QUARTER_FINAL,
    SEMI_FINAL,
    FINAL,
    WINNER;

    public static final List<Round> KNOCKOUT_ROUNDS = List.of(ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL);

    private static final Set<String> GROUP_STAGE_LABELS = Set.of("Phase de groupes", "Phase de ligue", "Barrages");

    public static boolean isGroupStageLabel(String label) {
        return label != null && GROUP_STAGE_LABELS.contains(label.trim());
    }

    public static Round fromDatabase(String lastRoundPlayed, boolean winner) {
        if (winner) {
            return WINNER;
        }
        if (isGroupStageLabel(lastRoundPlayed)) {
            return GROUP_STAGE;
        }
        return switch (lastRoundPlayed.trim()) {
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
