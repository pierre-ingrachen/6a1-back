package com.takima.backskeleton.game.exception;

public class UnknownFormationException extends RuntimeException {
    public UnknownFormationException(String label) {
        super("Unknown formation " + label);
    }
}
