package com.takima.backskeleton.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownFormationException extends RuntimeException {
    public UnknownFormationException(String label) {
        super("Unknown formation " + label);
    }
}
