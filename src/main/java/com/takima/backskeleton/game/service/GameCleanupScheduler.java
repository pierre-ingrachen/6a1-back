package com.takima.backskeleton.game.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GameCleanupScheduler {
    private final GameService gameService;

    public GameCleanupScheduler(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(fixedDelayString = "${game.cleanup.interval:PT10M}")
    public void removeIdleGames() {
        gameService.removeIdleGames();
    }
}
