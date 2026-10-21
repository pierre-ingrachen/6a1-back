package com.takima.backskeleton.game.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class GameLocks {
    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(UUID gameId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(gameId, id -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void release(UUID gameId) {
        locks.remove(gameId);
    }

    public int size() {
        return locks.size();
    }
}
