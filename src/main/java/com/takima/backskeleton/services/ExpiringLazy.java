package com.takima.backskeleton.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

final class ExpiringLazy<T> {
    private record Loaded<T>(T value, Instant loadedAt) {
    }

    private final Duration timeToLive;
    private final Clock clock;
    private volatile Loaded<T> loaded;

    ExpiringLazy(Duration timeToLive, Clock clock) {
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    T get(Supplier<T> loader) {
        Loaded<T> current = loaded;
        if (isUsable(current)) {
            return current.value();
        }
        synchronized (this) {
            current = loaded;
            if (!isUsable(current)) {
                current = new Loaded<>(loader.get(), clock.instant());
                loaded = current;
            }
            return current.value();
        }
    }

    private boolean isUsable(Loaded<T> candidate) {
        return candidate != null
                && (timeToLive.isZero() || timeToLive.isNegative() || candidate.loadedAt().plus(timeToLive).isAfter(clock.instant()));
    }
}
