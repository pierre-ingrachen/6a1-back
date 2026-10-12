package com.takima.backskeleton.game.model;

public record MatchResult(
        Round round,
        OpponentSummary opponent,
        int goalsFor,
        int goalsAgainst,
        boolean extraTime,
        Integer penaltiesFor,
        Integer penaltiesAgainst,
        boolean won
) {
    public static MatchResult of(Round round, OpponentSummary opponent, MatchOutcome outcome) {
        PenaltyShootout shootout = outcome.penalties();
        return new MatchResult(
                round,
                opponent,
                outcome.goalsA(),
                outcome.goalsB(),
                outcome.extraTime(),
                shootout == null ? null : shootout.scoredA(),
                shootout == null ? null : shootout.scoredB(),
                outcome.winnerIsA());
    }
}
