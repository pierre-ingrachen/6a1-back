package com.takima.backskeleton.exceptions;

public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(Integer teamId) {
        super("Team " + teamId + " not found");
    }
}
