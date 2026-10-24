package com.takima.backskeleton.game.service;

public final class GameBalance {
    public static final int MIN_DRAFT_MINUTES = 270;
    public static final int MIN_OPPONENT_MINUTES = 90;
    public static final int MISSING_PLAYER_RATING = 70;

    public static final double MIDFIELD_SHARE = 0.5;
    public static final double GOALKEEPER_WEIGHT = 1.5;

    public static final double BASE_GOALS = 1.35;
    public static final double GOAL_SENSITIVITY = 0.075;
    public static final double EXTRA_TIME_GOAL_SHARE = 1.0 / 3;
    public static final double PENALTY_SCORING_PROBABILITY = 0.75;

    public static final double PLAYER_HANDICAP = 2.0;

    public static final int MIN_DRAWABLE_STAR_RATING = 87;

    private GameBalance() {
    }
}
