package com.takima.backskeleton.game.exception;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID gameId) {
        super("Game " + gameId + " not found");
    }
}
