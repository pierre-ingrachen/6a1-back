package com.takima.backskeleton.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PlayerStatsNotFoundException extends RuntimeException {
    public PlayerStatsNotFoundException(Integer playerId, Short season) {
        super("No stats for player " + playerId + " in season " + season);
    }
}
