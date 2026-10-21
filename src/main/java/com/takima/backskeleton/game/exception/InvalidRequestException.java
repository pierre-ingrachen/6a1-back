package com.takima.backskeleton.game.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String reason) {
        super(reason);
    }
}
