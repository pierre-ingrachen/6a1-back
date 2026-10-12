package com.takima.backskeleton.game.model;

import java.util.List;

public record GroupStageResult(List<MatchResult> matches, List<GroupStanding> standings, int rank) {
}
