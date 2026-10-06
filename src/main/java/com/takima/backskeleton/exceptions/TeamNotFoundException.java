package com.takima.backskeleton.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(Integer teamId) {
        super("Team " + teamId + " not found");
    }
}
