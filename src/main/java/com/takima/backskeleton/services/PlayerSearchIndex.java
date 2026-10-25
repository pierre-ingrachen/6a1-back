package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.models.PlayerAppearance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class PlayerSearchIndex {
    private record IndexedAppearance(PlayerAppearance appearance, String normalizedName) {
    }

    private final PlayerSeasonStatsDao playerSeasonStatsDao;
    private final ExpiringLazy<List<IndexedAppearance>> indexedAppearances;

    public PlayerSearchIndex(PlayerSeasonStatsDao playerSeasonStatsDao, Clock clock,
                             @Value("${stats.cache.ttl:PT24H}") Duration timeToLive) {
        this.playerSeasonStatsDao = playerSeasonStatsDao;
        this.indexedAppearances = new ExpiringLazy<>(timeToLive, clock);
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public List<List<PlayerAppearance>> findAppearancesByPlayerName(String query) {
        String normalizedQuery = normalize(query);
        return indexedAppearances.get(this::load).stream()
                .filter(indexed -> indexed.normalizedName().contains(normalizedQuery))
                .map(IndexedAppearance::appearance)
                .collect(Collectors.groupingBy(PlayerAppearance::playerId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .toList();
    }

    public List<PlayerAppearance> findAppearancesByPlayerId(Integer playerId) {
        return indexedAppearances.get(this::load).stream()
                .map(IndexedAppearance::appearance)
                .filter(appearance -> playerId.equals(appearance.playerId()))
                .toList();
    }

    public void evict() {
        indexedAppearances.evict();
    }

    private List<IndexedAppearance> load() {
        return playerSeasonStatsDao.findAllAppearances().stream()
                .map(appearance -> new IndexedAppearance(appearance, normalize(appearance.playerName())))
                .toList();
    }
}
