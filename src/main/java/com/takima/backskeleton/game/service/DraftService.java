package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.DraftCompletedException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.PickedPlayer;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DraftService {
    private final TeamSeasonCatalog catalog;
    private final TeamDrawService teamDrawService;
    private final DraftEligibility eligibility;

    public DraftService(TeamSeasonCatalog catalog, TeamDrawService teamDrawService, DraftEligibility eligibility) {
        this.catalog = catalog;
        this.teamDrawService = teamDrawService;
        this.eligibility = eligibility;
    }

    public Game start(Formation formation, long seed) {
        Game game = Game.create(UUID.randomUUID(), seed, formation);
        return game.withDraw(teamDrawService.drawNext(game).key());
    }

    public Game pick(Game game, int playerId, int slotIndex) {
        requireDraftInProgress(game);
        TeamSeason drawn = currentTeamSeason(game);
        SeasonPlayer player = eligibility.draftablePlayers(drawn).stream()
                .filter(candidate -> candidate.playerId() == playerId)
                .findFirst()
                .orElseThrow(() -> new InvalidPickException("Player " + playerId + " is not available in the current draw"));
        if (game.hasPicked(playerId)) {
            throw new InvalidPickException("Player " + player.name() + " is already in your squad");
        }
        requireExistingSlot(game, slotIndex);
        if (game.picks().containsKey(slotIndex)) {
            throw new InvalidPickException("Slot " + slotIndex + " is already taken");
        }
        if (!eligibility.eligibleSlots(game, player).contains(slotIndex)) {
            throw new InvalidPickException(player.name() + " cannot play " + game.formation().slots().get(slotIndex));
        }

        Game updated = game.withPick(slotIndex, new PickedPlayer(
                player.playerId(), player.name(), player.rating(), drawn.teamName(), drawn.seasonLabel(),
                player.playablePositions()));
        return updated.isDraftComplete()
                ? updated.withoutDraw()
                : updated.withDraw(teamDrawService.drawNext(updated).key());
    }

    public Game redraw(Game game, RedrawType type) {
        requireDraftInProgress(game);
        if (game.redrawsLeft() <= 0) {
            throw new NoRedrawAvailableException("No redraw left");
        }
        TeamSeason redrawn = teamDrawService.redraw(game, type);
        return game.withRedrawUsed().withDraw(redrawn.key());
    }

    public Game move(Game game, int fromSlot, int toSlot) {
        PickedPlayer moved = game.picks().get(fromSlot);
        if (moved == null) {
            throw new InvalidPickException("Slot " + fromSlot + " has no player to move");
        }
        requireExistingSlot(game, toSlot);
        if (fromSlot == toSlot) {
            throw new InvalidPickException("Player is already at slot " + toSlot);
        }
        if (!moved.canPlay(game.formation().slots().get(toSlot))) {
            throw new InvalidPickException(moved.name() + " cannot play " + game.formation().slots().get(toSlot));
        }
        PickedPlayer displaced = game.picks().get(toSlot);
        if (displaced != null && !displaced.canPlay(game.formation().slots().get(fromSlot))) {
            throw new InvalidPickException(displaced.name() + " cannot play " + game.formation().slots().get(fromSlot));
        }

        Game updated = game.withMove(fromSlot, toSlot);
        boolean drawStillUseful = updated.currentDraw() == null
                || eligibility.offersPick(updated, currentTeamSeason(updated));
        return drawStillUseful ? updated : updated.withDraw(teamDrawService.drawNext(updated).key());
    }

    public TeamSeason currentTeamSeason(Game game) {
        return catalog.find(game.currentDraw())
                .orElseThrow(() -> new IllegalStateException("Drawn team season missing from catalog"));
    }

    private void requireDraftInProgress(Game game) {
        if (game.isDraftComplete()) {
            throw new DraftCompletedException(game.id());
        }
    }

    private void requireExistingSlot(Game game, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= game.formation().slots().size()) {
            throw new InvalidPickException("Slot " + slotIndex + " does not exist");
        }
    }
}
