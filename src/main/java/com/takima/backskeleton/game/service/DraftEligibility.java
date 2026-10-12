package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.FieldPosition;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DraftEligibility {

    public List<SeasonPlayer> draftablePlayers(TeamSeason teamSeason) {
        return teamSeason.players().stream()
                .filter(player -> player.minutes() >= GameBalance.MIN_DRAFT_MINUTES)
                .toList();
    }

    public List<Integer> eligibleSlots(Game game, SeasonPlayer player) {
        if (game.hasPicked(player.playerId())) {
            return List.of();
        }
        return game.openSlotIndexes().stream()
                .filter(index -> player.canPlay(slotPosition(game, index)))
                .toList();
    }

    public boolean offersPick(Game game, TeamSeason teamSeason) {
        return draftablePlayers(teamSeason).stream()
                .anyMatch(player -> !eligibleSlots(game, player).isEmpty());
    }

    private FieldPosition slotPosition(Game game, int slotIndex) {
        return game.formation().slots().get(slotIndex);
    }
}
