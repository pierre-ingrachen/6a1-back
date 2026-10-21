package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.game.support.MutableClock;
import com.takima.backskeleton.models.PlayerAppearance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerSearchIndexTest {
    @Mock
    private PlayerSeasonStatsDao dao;

    private final MutableClock clock = new MutableClock();
    private PlayerSearchIndex index;

    @BeforeEach
    void setUp() {
        index = new PlayerSearchIndex(dao, clock, Duration.ofHours(1));
        when(dao.findAllAppearances()).thenReturn(List.of(
                new PlayerAppearance(1, "Kylian Mbappé", "Forward", (short) 2025, "Real Madrid"),
                new PlayerAppearance(1, "Kylian Mbappé", "Forward", (short) 2022, "PSG"),
                new PlayerAppearance(2, "Bryan Mbeumo", "Forward", (short) 2025, "Manchester United")));
    }

    @Test
    void ignoresAccentsAndCaseAndGroupsAppearancesByPlayer() {
        List<List<PlayerAppearance>> matches = index.findAppearancesByPlayerName("MBAPPE");

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0)).extracting(PlayerAppearance::teamName).containsExactly("Real Madrid", "PSG");
    }

    @Test
    void loadsTheAppearancesOnlyOnce() {
        index.findAppearancesByPlayerName("mb");
        index.findAppearancesByPlayerName("bryan");

        verify(dao, times(1)).findAllAppearances();
    }

    @Test
    void reloadsAfterTheTimeToLive() {
        index.findAppearancesByPlayerName("mb");
        clock.advance(Duration.ofHours(2));

        index.findAppearancesByPlayerName("mb");

        verify(dao, times(2)).findAllAppearances();
    }

    @Test
    void returnsNothingWhenNobodyMatches() {
        assertThat(index.findAppearancesByPlayerName("zzz")).isEmpty();
    }
}
