package com.takima.backskeleton.game.model;

public record MatchOutcome(int goalsA, int goalsB, boolean extraTime, PenaltyShootout penalties) {

    public static MatchOutcome regular(int goalsA, int goalsB) {
        return new MatchOutcome(goalsA, goalsB, false, null);
    }

    public boolean winnerIsA() {
        if (goalsA != goalsB) {
            return goalsA > goalsB;
        }
        return penalties != null && penalties.scoredA() > penalties.scoredB();
    }

    public boolean isDraw() {
        return goalsA == goalsB && penalties == null;
    }
}
