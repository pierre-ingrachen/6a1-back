package com.takima.backskeleton.game.service;

public final class GameBalance {
    public static final int MIN_DRAFT_MINUTES = 270;
    public static final int MIN_OPPONENT_MINUTES = 90;
    public static final int MISSING_PLAYER_RATING = 62;

    public static final double MIDFIELD_SHARE = 0.5;
    public static final double GOALKEEPER_WEIGHT = 1.5;

    public static final double BASE_GOALS = 1.35;
    public static final double GOAL_SENSITIVITY = 0.06;
    public static final double EXTRA_TIME_GOAL_SHARE = 1.0 / 3;
    public static final double PENALTY_SCORING_PROBABILITY = 0.75;

    public static final double PLAYER_HANDICAP = 1.0;

    public static final int KEPT_EDITIONS_PERCENT = 40;

    private GameBalance() {
    }
}
