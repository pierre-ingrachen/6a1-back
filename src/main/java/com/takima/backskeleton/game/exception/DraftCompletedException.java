package com.takima.backskeleton.game.exception;

import java.util.UUID;

public class DraftCompletedException extends RuntimeException {
    public DraftCompletedException(UUID gameId) {
        super("Draft of game " + gameId + " is already complete");
    }
}
