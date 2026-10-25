package com.takima.backskeleton.services;

import com.takima.backskeleton.game.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiringLazyTest {
    private final MutableClock clock = new MutableClock();
    private final AtomicInteger loads = new AtomicInteger();

    @Test
    void loadsOnlyOnceWhileValid() {
        ExpiringLazy<Integer> lazy = new ExpiringLazy<>(Duration.ofHours(1), clock);

        lazy.get(loads::incrementAndGet);
        lazy.get(loads::incrementAndGet);

        assertThat(loads).hasValue(1);
    }

    @Test
    void reloadsOnceTheTimeToLiveHasElapsed() {
        ExpiringLazy<Integer> lazy = new ExpiringLazy<>(Duration.ofHours(1), clock);
        lazy.get(loads::incrementAndGet);

        clock.advance(Duration.ofHours(1).plusSeconds(1));

        assertThat(lazy.get(loads::incrementAndGet)).isEqualTo(2);
    }

    @Test
    void zeroTimeToLiveNeverExpires() {
        ExpiringLazy<Integer> lazy = new ExpiringLazy<>(Duration.ZERO, clock);
        lazy.get(loads::incrementAndGet);

        clock.advance(Duration.ofDays(3650));
        lazy.get(loads::incrementAndGet);

        assertThat(loads).hasValue(1);
    }
}
