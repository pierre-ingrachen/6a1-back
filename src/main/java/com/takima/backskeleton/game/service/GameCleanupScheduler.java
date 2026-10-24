package com.takima.backskeleton.game.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GameCleanupScheduler {
    private final GameService gameService;
    private final GameCreationRateLimiter gameCreationRateLimiter;

    public GameCleanupScheduler(GameService gameService, GameCreationRateLimiter gameCreationRateLimiter) {
        this.gameService = gameService;
        this.gameCreationRateLimiter = gameCreationRateLimiter;
    }

    @Scheduled(fixedDelayString = "${game.cleanup.interval:PT10M}")
    public void removeIdleGames() {
        gameService.removeIdleGames();
        gameCreationRateLimiter.purgeExpired();
    }
}
