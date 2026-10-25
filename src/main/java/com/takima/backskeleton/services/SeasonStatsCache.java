package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.models.PlayerSeasonStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SeasonStatsCache {
    private final PlayerSeasonStatsDao playerSeasonStatsDao;
    private final PercentileCalculator percentileCalculator;
    private final Clock clock;
    private final Duration timeToLive;
    private final Map<Short, ExpiringLazy<Optional<SeasonSnapshot>>> snapshotsBySeason = new ConcurrentHashMap<>();

    public SeasonStatsCache(PlayerSeasonStatsDao playerSeasonStatsDao, PercentileCalculator percentileCalculator, Clock clock,
                            @Value("${stats.cache.ttl:PT24H}") Duration timeToLive) {
        this.playerSeasonStatsDao = playerSeasonStatsDao;
        this.percentileCalculator = percentileCalculator;
        this.clock = clock;
        this.timeToLive = timeToLive;
    }

    public Optional<SeasonSnapshot> find(Short season) {
        ExpiringLazy<Optional<SeasonSnapshot>> holder =
                snapshotsBySeason.computeIfAbsent(season, key -> new ExpiringLazy<>(timeToLive, clock));
        Optional<SeasonSnapshot> snapshot = holder.get(() -> load(season));
        if (snapshot.isEmpty()) {
            snapshotsBySeason.remove(season, holder);
        }
        return snapshot;
    }

    int size() {
        return snapshotsBySeason.size();
    }

    private Optional<SeasonSnapshot> load(Short season) {
        List<SeasonPlayerStats> seasonPlayers = playerSeasonStatsDao.findBySeason(season).stream()
                .collect(Collectors.groupingBy(PlayerSeasonStats::playerId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(FirstTeamSelector::select)
                .toList();
        if (seasonPlayers.isEmpty()) {
            return Optional.empty();
        }
        Map<Integer, SeasonPlayerStats> playersById = seasonPlayers.stream()
                .collect(Collectors.toUnmodifiableMap(player -> player.stats().playerId(), Function.identity()));
        PercentileDistribution distribution = percentileCalculator.distributionOf(
                seasonPlayers.stream().map(SeasonPlayerStats::stats).toList());
        return Optional.of(new SeasonSnapshot(playersById, distribution));
    }
}
