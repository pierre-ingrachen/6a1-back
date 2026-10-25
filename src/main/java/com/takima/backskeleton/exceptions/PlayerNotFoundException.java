package com.takima.backskeleton.exceptions;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(Integer playerId) {
        super("No player " + playerId);
    }
}
