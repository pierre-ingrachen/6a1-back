package com.takima.backskeleton.exceptions;

public class PlayerStatsNotFoundException extends RuntimeException {
    public PlayerStatsNotFoundException(Integer playerId, Short season) {
        super("No stats for player " + playerId + " in season " + season);
    }
}
