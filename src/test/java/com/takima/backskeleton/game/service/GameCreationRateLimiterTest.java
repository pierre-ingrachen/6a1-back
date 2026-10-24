package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.GameCreationRateLimitedException;
import com.takima.backskeleton.game.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameCreationRateLimiterTest {
    private static final int MAX_REQUESTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final MutableClock clock = new MutableClock();
    private final GameCreationRateLimiter limiter = new GameCreationRateLimiter(clock, MAX_REQUESTS, WINDOW);

    @Test
    void requestsUpToTheLimitAreAccepted() {
        assertThatCode(() -> acquireTimes("10.0.0.1", MAX_REQUESTS)).doesNotThrowAnyException();
    }

    @Test
    void requestsBeyondTheLimitAreRejected() {
        acquireTimes("10.0.0.1", MAX_REQUESTS);

        assertThatThrownBy(() -> limiter.acquire("10.0.0.1")).isInstanceOf(GameCreationRateLimitedException.class);
    }

    @Test
    void eachClientHasItsOwnQuota() {
        acquireTimes("10.0.0.1", MAX_REQUESTS);

        assertThatCode(() -> limiter.acquire("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void quotaIsRestoredOnceTheWindowHasElapsed() {
        acquireTimes("10.0.0.1", MAX_REQUESTS);
        clock.advance(WINDOW);

        assertThatCode(() -> limiter.acquire("10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void rejectedRequestsDoNotExtendTheWindow() {
        acquireTimes("10.0.0.1", MAX_REQUESTS);
        clock.advance(WINDOW.minusSeconds(1));
        assertThatThrownBy(() -> limiter.acquire("10.0.0.1")).isInstanceOf(GameCreationRateLimitedException.class);
        clock.advance(Duration.ofSeconds(1));

        assertThatCode(() -> limiter.acquire("10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void purgeForgetsExpiredWindowsOnly() {
        limiter.acquire("10.0.0.1");
        clock.advance(WINDOW);
        limiter.acquire("10.0.0.2");

        limiter.purgeExpired();

        assertThat(limiter.size()).isEqualTo(1);
        acquireTimes("10.0.0.2", MAX_REQUESTS - 1);
        assertThatThrownBy(() -> limiter.acquire("10.0.0.2")).isInstanceOf(GameCreationRateLimitedException.class);
    }

    @Test
    void expiredWindowsAreForgottenWhenTooManyClientsAreTracked() {
        IntStream.range(0, GameCreationRateLimiter.PURGE_THRESHOLD).forEach(client -> limiter.acquire("client-" + client));
        clock.advance(WINDOW);
        acquireTimes("active-client", MAX_REQUESTS);
        limiter.acquire("trigger-client");

        assertThat(limiter.size()).isEqualTo(2);
        assertThatThrownBy(() -> limiter.acquire("active-client")).isInstanceOf(GameCreationRateLimitedException.class);
    }

    private void acquireTimes(String clientKey, int times) {
        IntStream.range(0, times).forEach(attempt -> limiter.acquire(clientKey));
    }
}
