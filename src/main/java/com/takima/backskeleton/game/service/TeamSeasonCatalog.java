package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dao.TeamSeasonSource;
import com.takima.backskeleton.game.model.Opponent;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamSeasonKey;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TeamSeasonCatalog {
    private final TeamSeasonSource source;
    private final TeamStrengthService strengthService;
    private volatile Content content;

    public TeamSeasonCatalog(TeamSeasonSource source, TeamStrengthService strengthService) {
        this.source = source;
        this.strengthService = strengthService;
    }

    public List<TeamSeason> teamSeasons() {
        return content().teamSeasons();
    }

    public List<TeamSeason> drawableTeamSeasons() {
        return content().drawable();
    }

    public List<Opponent> opponents() {
        return content().opponents();
    }

    public Optional<TeamSeason> find(TeamSeasonKey key) {
        return Optional.ofNullable(content().byKey().get(key));
    }

    private Content content() {
        Content loaded = content;
        if (loaded == null) {
            synchronized (this) {
                if (content == null) {
                    content = load();
                }
                loaded = content;
            }
        }
        return loaded;
    }

    private Content load() {
        List<TeamSeason> teamSeasons = source.loadAll();
        Map<TeamSeasonKey, TeamSeason> byKey = teamSeasons.stream()
                .collect(Collectors.toUnmodifiableMap(TeamSeason::key, Function.identity()));
        List<Opponent> opponents = teamSeasons.stream()
                .map(teamSeason -> new Opponent(teamSeason, strengthService.ofOpponent(teamSeason)))
                .toList();
        return new Content(teamSeasons, byKey, opponents, selectDrawable(opponents));
    }

    private List<TeamSeason> selectDrawable(List<Opponent> opponents) {
        Set<TeamSeasonKey> kept = opponents.stream()
                .filter(opponent -> opponent.teamSeason().roundReached().hasPlayed(Round.ROUND_OF_16))
                .collect(Collectors.groupingBy(opponent -> opponent.teamSeason().teamId()))
                .values().stream()
                .flatMap(editions -> editions.stream()
                        .sorted(Comparator.comparingDouble((Opponent opponent) -> opponent.strength().overall()).reversed())
                        .limit(keptEditionsCount(editions.size())))
                .map(opponent -> opponent.teamSeason().key())
                .collect(Collectors.toSet());
        return opponents.stream()
                .map(Opponent::teamSeason)
                .filter(teamSeason -> kept.contains(teamSeason.key()))
                .toList();
    }

    private long keptEditionsCount(int editions) {
        return (editions * GameBalance.KEPT_EDITIONS_PERCENT + 99) / 100;
    }

    private record Content(List<TeamSeason> teamSeasons, Map<TeamSeasonKey, TeamSeason> byKey, List<Opponent> opponents,
                           List<TeamSeason> drawable) {
    }
}
