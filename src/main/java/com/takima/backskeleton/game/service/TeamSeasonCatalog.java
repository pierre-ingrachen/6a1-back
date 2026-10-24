package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dao.TeamSeasonSource;
import com.takima.backskeleton.game.model.Opponent;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamSeasonKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return new Content(teamSeasons, byKey, opponents, selectDrawable(teamSeasons));
    }

    private List<TeamSeason> selectDrawable(List<TeamSeason> teamSeasons) {
        return teamSeasons.stream()
                .filter(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16))
                .filter(this::hasStar)
                .toList();
    }

    private boolean hasStar(TeamSeason teamSeason) {
        return teamSeason.players().stream()
                .filter(player -> player.minutes() >= GameBalance.MIN_DRAFT_MINUTES)
                .anyMatch(player -> player.rating() >= GameBalance.MIN_DRAWABLE_STAR_RATING);
    }

    private record Content(List<TeamSeason> teamSeasons, Map<TeamSeasonKey, TeamSeason> byKey, List<Opponent> opponents,
                           List<TeamSeason> drawable) {
    }
}
