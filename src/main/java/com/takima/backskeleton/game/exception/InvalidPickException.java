package com.takima.backskeleton.game.exception;

public class InvalidPickException extends RuntimeException {
    public InvalidPickException(String reason) {
        super(reason);
    }
}
