package com.takima.backskeleton.game.exception;

public class GameCreationRateLimitedException extends RuntimeException {
    public GameCreationRateLimitedException() {
        super("Too many game creations, try again later");
    }
}
