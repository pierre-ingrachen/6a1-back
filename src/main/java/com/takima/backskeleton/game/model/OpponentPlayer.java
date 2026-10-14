package com.takima.backskeleton.game.model;

public record OpponentPlayer(String name, int rating, Line line) {
    public static OpponentPlayer from(SeasonPlayer player) {
        return new OpponentPlayer(player.name(), player.rating(), player.ratingLine());
    }
}
