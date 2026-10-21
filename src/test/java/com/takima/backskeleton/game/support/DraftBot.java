package com.takima.backskeleton.game.support;

import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.service.DraftEligibility;
import com.takima.backskeleton.game.service.DraftService;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class DraftBot {
    private final DraftService draftService;
    private final DraftEligibility eligibility;
    private final Random random;

    public DraftBot(DraftService draftService, DraftEligibility eligibility, Random random) {
        this.draftService = draftService;
        this.eligibility = eligibility;
        this.random = random;
    }

    public Game playStarSeeker(Game game, int redrawBelowRating) {
        Game current = game;
        while (!current.isDraftComplete()) {
            List<SeasonPlayer> options = pickableOptions(current);
            int bestRating = options.stream().mapToInt(SeasonPlayer::rating).max().orElse(0);
            if (current.redrawsLeft() > 0 && bestRating < redrawBelowRating) {
                try {
                    current = draftService.redraw(current, randomRedrawType());
                    continue;
                } catch (NoRedrawAvailableException exception) {
                }
            }
            SeasonPlayer best = options.stream().max(Comparator.comparingInt(SeasonPlayer::rating)).orElseThrow();
            current = pick(current, best);
        }
        return current;
    }

    public Game playRandomly(Game game) {
        Game current = game;
        while (!current.isDraftComplete()) {
            List<SeasonPlayer> options = pickableOptions(current);
            current = pick(current, options.get(random.nextInt(options.size())));
        }
        return current;
    }

    private Game pick(Game game, SeasonPlayer player) {
        int slotIndex = eligibility.eligibleSlots(game, player).get(0);
        return draftService.pick(game, player.playerId(), slotIndex);
    }

    private List<SeasonPlayer> pickableOptions(Game game) {
        return eligibility.draftablePlayers(draftService.currentTeamSeason(game)).stream()
                .filter(player -> !eligibility.eligibleSlots(game, player).isEmpty())
                .toList();
    }

    private RedrawType randomRedrawType() {
        return random.nextBoolean() ? RedrawType.SEASON : RedrawType.TEAM;
    }
}
