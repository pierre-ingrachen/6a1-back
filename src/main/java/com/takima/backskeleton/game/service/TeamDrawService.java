package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamSeasonKey;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

@Service
public class TeamDrawService {
    private static final long DRAW_SEED_STRIDE = 7919L;

    private final TeamSeasonCatalog catalog;
    private final DraftEligibility eligibility;

    public TeamDrawService(TeamSeasonCatalog catalog, DraftEligibility eligibility) {
        this.catalog = catalog;
        this.eligibility = eligibility;
    }

    public TeamSeason drawNext(Game game) {
        return pickWeighted(game, candidates(game, teamSeason -> true))
                .orElseThrow(() -> new NoRedrawAvailableException("No team available to draw"));
    }

    public TeamSeason redraw(Game game, RedrawType type) {
        TeamSeasonKey current = game.currentDraw();
        Predicate<TeamSeason> sameFamily = switch (type) {
            case SEASON -> teamSeason -> teamSeason.teamId() == current.teamId() && teamSeason.startYear() != current.startYear();
            case TEAM -> teamSeason -> teamSeason.startYear() == current.startYear() && teamSeason.teamId() != current.teamId();
        };
        return pickWeighted(game, candidates(game, sameFamily))
                .orElseThrow(() -> new NoRedrawAvailableException("No other " + type.name().toLowerCase() + " available to draw"));
    }

    private List<TeamSeason> candidates(Game game, Predicate<TeamSeason> filter) {
        return catalog.drawableTeamSeasons().stream()
                .filter(teamSeason -> !game.seenDraws().contains(teamSeason.key()))
                .filter(filter)
                .filter(teamSeason -> eligibility.offersPick(game, teamSeason))
                .toList();
    }

    private Optional<TeamSeason> pickWeighted(Game game, List<TeamSeason> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Random random = new Random(game.seed() + game.drawCount() * DRAW_SEED_STRIDE);
        double totalWeight = candidates.stream().mapToDouble(this::weight).sum();
        double target = random.nextDouble() * totalWeight;
        double cumulated = 0;
        for (TeamSeason candidate : candidates) {
            cumulated += weight(candidate);
            if (target < cumulated) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(candidates.get(candidates.size() - 1));
    }

    private double weight(TeamSeason teamSeason) {
        return 1 + GameBalance.DRAW_WEIGHT_PER_ROUND * teamSeason.roundReached().ordinal();
    }
}
