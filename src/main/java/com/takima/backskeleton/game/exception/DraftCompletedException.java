package com.takima.backskeleton.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.CONFLICT)
public class DraftCompletedException extends RuntimeException {
    public DraftCompletedException(UUID gameId) {
        super("Draft of game " + gameId + " is already complete");
    }
}
