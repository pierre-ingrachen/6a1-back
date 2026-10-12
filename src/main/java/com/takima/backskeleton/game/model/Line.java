package com.takima.backskeleton.game.model;

public enum Line {
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    ATTACKER;

    public static Line fromDatabase(String position) {
        return switch (position.trim()) {
            case "Goalkeeper" -> GOALKEEPER;
            case "Defender" -> DEFENDER;
            case "Midfielder" -> MIDFIELDER;
            case "Forward" -> ATTACKER;
            default -> throw new IllegalArgumentException("Unknown position " + position);
        };
    }
}
