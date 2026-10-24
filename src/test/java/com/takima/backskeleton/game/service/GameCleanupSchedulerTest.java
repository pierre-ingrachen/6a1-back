package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GameCleanupSchedulerTest {
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final MutableClock clock = new MutableClock();
    private final GameService gameService = mock(GameService.class);
    private final GameCreationRateLimiter limiter = new GameCreationRateLimiter(clock, 5, WINDOW);
    private final GameCleanupScheduler scheduler = new GameCleanupScheduler(gameService, limiter);

    @Test
    void removesIdleGames() {
        scheduler.removeIdleGames();

        verify(gameService).removeIdleGames();
    }

    @Test
    void forgetsExpiredRateLimitWindows() {
        limiter.acquire("10.0.0.1");
        clock.advance(WINDOW);

        scheduler.removeIdleGames();

        assertThat(limiter.size()).isZero();
    }
}
