package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerSeasonStatsDao;
import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerSeasonTeamDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.DTO.StatCategoryDto;
import com.takima.backskeleton.DTO.StatDto;
import com.takima.backskeleton.DTO.StatValueDto;
import com.takima.backskeleton.exceptions.PlayerStatsNotFoundException;
import com.takima.backskeleton.models.PlayerAppearance;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import com.takima.backskeleton.models.StatCategory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
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
    static final int MINIMUM_MINUTES_FOR_PERCENTILES = 450;
    static final int MAXIMUM_SEARCH_RESULTS = 20;
    private static final int MINIMUM_QUERY_LENGTH = 2;
    private static final String GOALKEEPER = "Goalkeeper";
    private static final Set<String> GROUP_STAGE_ELIMINATIONS = Set.of("Phase de groupes", "Phase de ligue");
    private static final List<StatCategory> FORWARD_CATEGORY_ORDER = List.of(
            GOALS, SHOTS, CREATION, DRIBBLING, PASSING, CROSSES_AND_SET_PIECES, AERIAL_DUELS, DEFENDING, DISCIPLINE, GOALKEEPING, PLAYING_TIME);
    private static final Map<String, List<StatCategory>> CATEGORY_ORDER_BY_POSITION = Map.of(
            "Forward", FORWARD_CATEGORY_ORDER,
            "Midfielder", List.of(CREATION, PASSING, DRIBBLING, DEFENDING, GOALS, SHOTS, CROSSES_AND_SET_PIECES, AERIAL_DUELS, DISCIPLINE, GOALKEEPING, PLAYING_TIME),
            "Defender", List.of(DEFENDING, AERIAL_DUELS, PASSING, DISCIPLINE, CREATION, DRIBBLING, CROSSES_AND_SET_PIECES, GOALS, SHOTS, GOALKEEPING, PLAYING_TIME),
            GOALKEEPER, List.of(GOALKEEPING, PASSING, AERIAL_DUELS, DEFENDING, DISCIPLINE, PLAYING_TIME, CREATION, DRIBBLING, CROSSES_AND_SET_PIECES, GOALS, SHOTS)
    );
    private static final StatValueDto NO_VALUE = new StatValueDto(null, null, null);

    private final PlayerSeasonStatsDao playerSeasonStatsDao;

    public PlayerService(PlayerSeasonStatsDao playerSeasonStatsDao) {
        this.playerSeasonStatsDao = playerSeasonStatsDao;
    }

    public List<PlayerSearchResultDto> searchPlayers(String query) {
        String normalizedQuery = normalize(query.trim());
        if (normalizedQuery.length() < MINIMUM_QUERY_LENGTH) {
            return List.of();
        }
        return playerSeasonStatsDao.findAllAppearances().stream()
                .filter(appearance -> normalize(appearance.playerName()).contains(normalizedQuery))
                .collect(Collectors.groupingBy(PlayerAppearance::playerId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toSearchResult)
                .sorted(Comparator.comparing(PlayerSearchResultDto::name))
                .limit(MAXIMUM_SEARCH_RESULTS)
                .toList();
    }

    private PlayerSearchResultDto toSearchResult(List<PlayerAppearance> appearances) {
        PlayerAppearance latest = appearances.stream()
                .max(Comparator.comparing(PlayerAppearance::season))
                .orElseThrow();
        List<PlayerSeasonTeamDto> seasons = appearances.stream()
                .collect(Collectors.toMap(PlayerAppearance::season, PlayerAppearance::teamName, (first, second) -> first, LinkedHashMap::new))
                .entrySet().stream()
                .map(entry -> new PlayerSeasonTeamDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PlayerSeasonTeamDto::startYear).reversed())
                .toList();
        return new PlayerSearchResultDto(latest.playerId(), latest.playerName(), latest.position(), latest.teamName(), seasons);
    }

    public PlayerStatsDto findPlayerStats(Integer playerId, Short season) {
        List<SeasonPlayer> seasonPlayers = findSeasonPlayers(season);
        SeasonPlayer seasonPlayer = seasonPlayers.stream()
                .filter(candidate -> candidate.stats().playerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new PlayerStatsNotFoundException(playerId, season));
        List<PlayerSeasonStats> population = seasonPlayers.stream()
                .map(SeasonPlayer::stats)
                .filter(stats -> stats.minutes() >= MINIMUM_MINUTES_FOR_PERCENTILES)
                .filter(stats -> !stats.playerId().equals(playerId))
                .toList();
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
                toCategoryDtos(stats, population)
        );
    }

    private List<SeasonPlayer> findSeasonPlayers(Short season) {
        return playerSeasonStatsDao.findBySeason(season).stream()
                .collect(Collectors.groupingBy(PlayerSeasonStats::playerId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::selectFirstTeam)
                .toList();
    }

    private SeasonPlayer selectFirstTeam(List<PlayerSeasonStats> teamsOfSeason) {
        if (teamsOfSeason.size() == 1) {
            return new SeasonPlayer(teamsOfSeason.get(0), false);
        }
        List<PlayerSeasonStats> eliminatedInGroupStage = teamsOfSeason.stream()
                .filter(stats -> GROUP_STAGE_ELIMINATIONS.contains(Objects.toString(stats.eliminationRound(), "")))
                .toList();
        if (eliminatedInGroupStage.size() == 1) {
            return new SeasonPlayer(eliminatedInGroupStage.get(0), false);
        }
        PlayerSeasonStats mostMatchesPlayed = teamsOfSeason.stream()
                .max(Comparator.comparingInt(PlayerSeasonStats::matchesPlayed).thenComparingInt(PlayerSeasonStats::minutes))
                .orElseThrow();
        return new SeasonPlayer(mostMatchesPlayed, true);
    }

    private List<StatCategoryDto> toCategoryDtos(PlayerSeasonStats player, List<PlayerSeasonStats> population) {
        return CATEGORY_ORDER_BY_POSITION.getOrDefault(player.position(), FORWARD_CATEGORY_ORDER).stream()
                .map(category -> new StatCategoryDto(
                        category.name(),
                        category.getLabel(),
                        Stat.byCategory(category).stream()
                                .map(stat -> toStatDto(stat, player, population))
                                .toList()))
                .toList();
    }

    private StatDto toStatDto(Stat stat, PlayerSeasonStats player, List<PlayerSeasonStats> population) {
        if (stat.getCategory() == GOALKEEPING && !isGoalkeeper(player)) {
            return toStatDto(stat, NO_VALUE, NO_VALUE);
        }
        List<PlayerSeasonStats> comparablePlayers = stat.getCategory() == GOALKEEPING
                ? population.stream().filter(this::isGoalkeeper).toList()
                : population;
        StatValueDto total = rank(stat, player, comparablePlayers, this::totalValue);
        StatValueDto per90 = stat.isPer90Applicable() ? rank(stat, player, comparablePlayers, this::per90Value) : total;
        return toStatDto(stat, total, per90);
    }

    private StatDto toStatDto(Stat stat, StatValueDto total, StatValueDto per90) {
        return new StatDto(stat.name(), stat.getLabel(), stat.isDetail(), stat.isLowerBetter(), total, per90);
    }

    private StatValueDto rank(Stat stat, PlayerSeasonStats player, List<PlayerSeasonStats> population,
                              BiFunction<Stat, PlayerSeasonStats, Double> valueOf) {
        Double value = valueOf.apply(stat, player);
        if (value == null) {
            return NO_VALUE;
        }
        List<Double> otherValues = population.stream()
                .map(other -> valueOf.apply(stat, other))
                .filter(Objects::nonNull)
                .toList();
        if (otherValues.isEmpty()) {
            return new StatValueDto(value, null, null);
        }
        long worseCount = otherValues.stream()
                .filter(other -> stat.isLowerBetter() ? other > value : other < value)
                .count();
        long tiedCount = otherValues.stream()
                .filter(other -> Double.compare(other, value) == 0)
                .count();
        int percentile = (int) Math.round(100.0 * (worseCount + tiedCount / 2.0) / otherValues.size());
        return new StatValueDto(value, percentile, Math.max(1, 100 - percentile));
    }

    private Double totalValue(Stat stat, PlayerSeasonStats player) {
        if (!stat.isRatio()) {
            return player.value(stat.getColumn());
        }
        Double attempts = player.value(stat.getDenominatorColumn());
        Double successes = player.value(stat.getNumeratorColumn());
        if (attempts == null || successes == null || attempts < stat.getMinimumAttempts()) {
            return null;
        }
        return 100.0 * successes / attempts;
    }

    private Double per90Value(Stat stat, PlayerSeasonStats player) {
        Double total = totalValue(stat, player);
        if (total == null || player.minutes() == 0) {
            return null;
        }
        return total * 90 / player.minutes();
    }

    private boolean isGoalkeeper(PlayerSeasonStats player) {
        return GOALKEEPER.equals(player.position());
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record SeasonPlayer(PlayerSeasonStats stats, boolean teamEstimated) {
    }
}
