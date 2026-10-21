package com.takima.backskeleton.game.exception;

import java.util.UUID;

public class DraftIncompleteException extends RuntimeException {
    public DraftIncompleteException(UUID gameId) {
        super("Draft of game " + gameId + " is not complete");
    }
}
