package com.takima.backskeleton.game.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GameLocksTest {
    private static final long TIMEOUT_SECONDS = 5;

    private final GameLocks gameLocks = new GameLocks();

    @Test
    void differentGamesDoNotBlockEachOther() throws Exception {
        UUID firstGame = UUID.randomUUID();
        UUID secondGame = UUID.randomUUID();
        CountDownLatch firstGameLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstGame = new CountDownLatch(1);
        CompletableFuture<Boolean> holder = CompletableFuture.supplyAsync(() -> gameLocks.withLock(firstGame, () -> {
            firstGameLocked.countDown();
            return await(releaseFirstGame);
        }));
        assertThat(firstGameLocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        String secondResult = CompletableFuture.supplyAsync(() -> gameLocks.withLock(secondGame, () -> "done"))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(secondResult).isEqualTo("done");
        releaseFirstGame.countDown();
        assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void sameGameIsSerialized() throws Exception {
        UUID game = UUID.randomUUID();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Boolean> holder = CompletableFuture.supplyAsync(() -> gameLocks.withLock(game, () -> {
            locked.countDown();
            return await(release);
        }));
        assertThat(locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<String> waiter = CompletableFuture.supplyAsync(() -> gameLocks.withLock(game, () -> "done"));

        Thread.sleep(200);
        assertThat(waiter).isNotDone();
        release.countDown();
        assertThat(waiter.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo("done");
        assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void releaseRemovesTheEntry() {
        UUID game = UUID.randomUUID();
        gameLocks.withLock(game, () -> null);

        gameLocks.release(game);

        assertThat(gameLocks.size()).isZero();
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
