package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerSeasonTeamDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.DTO.StatCategoryDto;
import com.takima.backskeleton.DTO.StatDto;
import com.takima.backskeleton.exceptions.PlayerNotFoundException;
import com.takima.backskeleton.exceptions.PlayerStatsNotFoundException;
import com.takima.backskeleton.models.PlayerAppearance;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import com.takima.backskeleton.models.StatCategory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.takima.backskeleton.models.StatCategory.AERIAL_DUELS;
import static com.takima.backskeleton.models.StatCategory.CREATION;
import static com.takima.backskeleton.models.StatCategory.CROSSES_AND_SET_PIECES;
import static com.takima.backskeleton.models.StatCategory.DEFENDING;
import static com.takima.backskeleton.models.StatCategory.DISCIPLINE;
import static com.takima.backskeleton.models.StatCategory.DRIBBLING;
import static com.takima.backskeleton.models.StatCategory.GOALKEEPING;
import static com.takima.backskeleton.models.StatCategory.GOALS;
import static com.takima.backskeleton.models.StatCategory.PASSING;
import static com.takima.backskeleton.models.StatCategory.PLAYING_TIME;
import static com.takima.backskeleton.models.StatCategory.SHOTS;

@Service
public class PlayerService {
    static final int MAXIMUM_SEARCH_RESULTS = 20;
    private static final int MINIMUM_QUERY_LENGTH = 2;
    private static final String GOALKEEPER = "Goalkeeper";
    private static final List<StatCategory> FORWARD_CATEGORY_ORDER = List.of(
            GOALS, SHOTS, CREATION, DRIBBLING, PASSING, CROSSES_AND_SET_PIECES, AERIAL_DUELS, DEFENDING, DISCIPLINE, GOALKEEPING, PLAYING_TIME);
    private static final Map<String, List<StatCategory>> CATEGORY_ORDER_BY_POSITION = Map.of(
            "Forward", FORWARD_CATEGORY_ORDER,
            "Midfielder", List.of(CREATION, PASSING, DRIBBLING, DEFENDING, GOALS, SHOTS, CROSSES_AND_SET_PIECES, AERIAL_DUELS, DISCIPLINE, GOALKEEPING, PLAYING_TIME),
            "Defender", List.of(DEFENDING, AERIAL_DUELS, PASSING, DISCIPLINE, CREATION, DRIBBLING, CROSSES_AND_SET_PIECES, GOALS, SHOTS, GOALKEEPING, PLAYING_TIME),
            GOALKEEPER, List.of(GOALKEEPING, PASSING, AERIAL_DUELS, DEFENDING, DISCIPLINE, PLAYING_TIME, CREATION, DRIBBLING, CROSSES_AND_SET_PIECES, GOALS, SHOTS)
    );

    private final PlayerSearchIndex playerSearchIndex;
    private final SeasonStatsCache seasonStatsCache;
    private final PercentileCalculator percentileCalculator;

    public PlayerService(PlayerSearchIndex playerSearchIndex, SeasonStatsCache seasonStatsCache,
                         PercentileCalculator percentileCalculator) {
        this.playerSearchIndex = playerSearchIndex;
        this.seasonStatsCache = seasonStatsCache;
        this.percentileCalculator = percentileCalculator;
    }

    public List<PlayerSearchResultDto> searchPlayers(Short season, String query) {
        String trimmedQuery = query.trim();
        if (PlayerSearchIndex.normalize(trimmedQuery).length() < MINIMUM_QUERY_LENGTH) {
            return List.of();
        }
        return playerSearchIndex.findAppearancesByPlayerName(trimmedQuery).stream()
                .filter(appearances -> season == null || appearances.stream().anyMatch(appearance -> season.equals(appearance.season())))
                .map(appearances -> toSearchResult(appearances, season))
                .sorted(Comparator.comparing(PlayerSearchResultDto::name))
                .limit(MAXIMUM_SEARCH_RESULTS)
                .toList();
    }

    public void evictCaches() {
        playerSearchIndex.evict();
        seasonStatsCache.evictAll();
    }

    private PlayerSearchResultDto toSearchResult(List<PlayerAppearance> appearances, Short season) {
        PlayerAppearance latest = appearances.stream()
                .filter(appearance -> season == null || season.equals(appearance.season()))
                .max(Comparator.comparing(PlayerAppearance::season))
                .orElseThrow();
        return new PlayerSearchResultDto(latest.playerId(), latest.playerName(), latest.position(), latest.teamName(), toSeasonTeams(appearances));
    }

    private List<PlayerSeasonTeamDto> toSeasonTeams(List<PlayerAppearance> appearances) {
        return appearances.stream()
                .collect(Collectors.toMap(PlayerAppearance::season, PlayerAppearance::teamName, (first, second) -> first, LinkedHashMap::new))
                .entrySet().stream()
                .map(entry -> new PlayerSeasonTeamDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PlayerSeasonTeamDto::startYear).reversed())
                .toList();
    }

    public List<PlayerSeasonTeamDto> findPlayerSeasons(Integer playerId) {
        List<PlayerAppearance> appearances = playerSearchIndex.findAppearancesByPlayerId(playerId);
        if (appearances.isEmpty()) {
            throw new PlayerNotFoundException(playerId);
        }
        return toSeasonTeams(appearances);
    }

    public PlayerStatsDto findPlayerStats(Integer playerId, Short season) {
        SeasonSnapshot snapshot = seasonStatsCache.find(season)
                .orElseThrow(() -> new PlayerStatsNotFoundException(playerId, season));
        SeasonPlayerStats seasonPlayer = Optional.ofNullable(snapshot.playersById().get(playerId))
                .orElseThrow(() -> new PlayerStatsNotFoundException(playerId, season));
        PlayerSeasonStats stats = seasonPlayer.stats();
        return new PlayerStatsDto(
                stats.playerId(),
                stats.playerName(),
                stats.position(),
                stats.heightCm(),
                stats.weightKg(),
                stats.teamName(),
                seasonPlayer.teamEstimated(),
                stats.rating(),
                stats.averageRating(),
                stats.minutes(),
                toCategoryDtos(stats, snapshot.distribution())
        );
    }

    private List<StatCategoryDto> toCategoryDtos(PlayerSeasonStats player, PercentileDistribution distribution) {
        return CATEGORY_ORDER_BY_POSITION.getOrDefault(player.position(), FORWARD_CATEGORY_ORDER).stream()
                .map(category -> new StatCategoryDto(
                        category.name(),
                        category.getLabel(),
                        Stat.byCategory(category).stream()
                                .map(stat -> new StatDto(
                                        stat.name(), stat.getLabel(), stat.isDetail(), stat.isLowerBetter(),
                                        percentileCalculator.total(stat, player, distribution),
                                        percentileCalculator.per90(stat, player, distribution)))
                                .toList()))
                .toList();
    }
}
