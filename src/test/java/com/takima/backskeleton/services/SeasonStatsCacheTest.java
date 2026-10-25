package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.game.support.MutableClock;
import com.takima.backskeleton.models.PlayerSeasonStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonStatsCacheTest {
    private static final Short SEASON = 2025;

    @Mock
    private PlayerSeasonStatsDao dao;

    private final MutableClock clock = new MutableClock();
    private SeasonStatsCache cache;

    @BeforeEach
    void setUp() {
        cache = new SeasonStatsCache(dao, new PercentileCalculator(), clock, Duration.ofHours(1));
    }

    @Test
    void loadsASeasonOnlyOnce() {
        when(dao.findBySeason(SEASON)).thenReturn(List.of(player(1, 900)));

        cache.find(SEASON);
        cache.find(SEASON);

        verify(dao, times(1)).findBySeason(SEASON);
    }

    @Test
    void indexesPlayersById() {
        when(dao.findBySeason(SEASON)).thenReturn(List.of(player(1, 900), player(2, 900)));

        SeasonSnapshot snapshot = cache.find(SEASON).orElseThrow();

        assertThat(snapshot.playersById()).containsOnlyKeys(1, 2);
    }

    @Test
    void doesNotKeepEmptySeasons() {
        when(dao.findBySeason(SEASON)).thenReturn(List.of());

        assertThat(cache.find(SEASON)).isEmpty();

        assertThat(cache.size()).isZero();
    }

    @Test
    void reloadsAfterTheTimeToLive() {
        when(dao.findBySeason(SEASON)).thenReturn(List.of(player(1, 900)));
        cache.find(SEASON);

        clock.advance(Duration.ofHours(2));
        cache.find(SEASON);

        verify(dao, times(2)).findBySeason(SEASON);
    }

    @Test
    void aSlowLoadDoesNotBlockOtherSeasons() throws Exception {
        Short slowSeason = 2020;
        CountDownLatch slowLoadStarted = new CountDownLatch(1);
        CountDownLatch releaseSlowLoad = new CountDownLatch(1);
        when(dao.findBySeason(slowSeason)).thenAnswer(invocation -> {
            slowLoadStarted.countDown();
            releaseSlowLoad.await(5, TimeUnit.SECONDS);
            return List.of(player(1, 900));
        });
        when(dao.findBySeason(SEASON)).thenReturn(List.of(player(2, 900)));
        CompletableFuture<?> slow = CompletableFuture.runAsync(() -> cache.find(slowSeason));
        assertThat(slowLoadStarted.await(5, TimeUnit.SECONDS)).isTrue();

        Object fast = CompletableFuture.supplyAsync(() -> cache.find(SEASON)).get(2, TimeUnit.SECONDS);

        assertThat(fast).isNotNull();
        releaseSlowLoad.countDown();
        slow.get(5, TimeUnit.SECONDS);
    }

    private static PlayerSeasonStats player(int id, int minutes) {
        return new PlayerSeasonStats(id, "Player " + id, "Forward", null, null, "Team", null, null, null,
                Map.of("minutes", (double) minutes, "matchs_joues", 6.0));
    }
}
