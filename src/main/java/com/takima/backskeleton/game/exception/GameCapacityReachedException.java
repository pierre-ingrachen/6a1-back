package com.takima.backskeleton.game.exception;

public class GameCapacityReachedException extends RuntimeException {
    public GameCapacityReachedException(int maxActiveGames) {
        super("Too many active games (" + maxActiveGames + "), try again later");
    }
}
