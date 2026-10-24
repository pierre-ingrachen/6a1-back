package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.GameCreationRateLimitedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameCreationRateLimiter {
    private record Window(Instant start, int count) {
        boolean isOpenAt(Instant instant, Duration length) {
            return instant.isBefore(start.plus(length));
        }
    }

    static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxRequests;
    private final Duration windowLength;

    public GameCreationRateLimiter(Clock clock,
                                   @Value("${game.creation.rate-limit.max-requests:20}") int maxRequests,
                                   @Value("${game.creation.rate-limit.window:PT1M}") Duration windowLength) {
        this.clock = clock;
        this.maxRequests = maxRequests;
        this.windowLength = windowLength;
    }

    public void acquire(String clientKey) {
        if (windows.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }
        Instant now = clock.instant();
        Window window = windows.compute(clientKey, (key, current) ->
                current != null && current.isOpenAt(now, windowLength)
                        ? new Window(current.start(), current.count() + 1)
                        : new Window(now, 1));
        if (window.count() > maxRequests) {
            throw new GameCreationRateLimitedException();
        }
    }

    public void purgeExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(window -> !window.isOpenAt(now, windowLength));
    }

    public int size() {
        return windows.size();
    }
}
