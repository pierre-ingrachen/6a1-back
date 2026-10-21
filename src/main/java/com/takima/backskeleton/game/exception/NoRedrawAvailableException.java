package com.takima.backskeleton.game.exception;

public class NoRedrawAvailableException extends RuntimeException {
    public NoRedrawAvailableException(String reason) {
        super(reason);
    }
}
