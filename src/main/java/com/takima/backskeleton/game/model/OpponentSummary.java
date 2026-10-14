package com.takima.backskeleton.game.model;

import java.util.List;

public record OpponentSummary(int teamId, String teamName, String seasonLabel, int rating, List<OpponentPlayer> players) {
}
