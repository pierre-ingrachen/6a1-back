package com.takima.backskeleton.game.model;

import java.util.List;

public record CompetitionResult(
        TeamStrength squadStrength,
        GroupStageResult groupStage,
        List<MatchResult> knockoutMatches,
        Round outcome,
        boolean champion
) {
}
