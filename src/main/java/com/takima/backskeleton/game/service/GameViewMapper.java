package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dto.FormationDto;
import com.takima.backskeleton.game.dto.GameStatus;
import com.takima.backskeleton.game.dto.GameView;
import com.takima.backskeleton.game.model.FieldPosition;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.PickedPlayer;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class GameViewMapper {
    private final TeamSeasonCatalog catalog;
    private final DraftEligibility eligibility;

    public GameViewMapper(TeamSeasonCatalog catalog, DraftEligibility eligibility) {
        this.catalog = catalog;
        this.eligibility = eligibility;
    }

    public GameView toView(Game game) {
        return new GameView(
                game.id(),
                game.formation().label(),
                statusOf(game),
                game.picksRemaining(),
                game.redrawsLeft(),
                averageRating(game),
                slotsOf(game),
                game.currentDraw() == null ? null : drawOf(game));
    }

    public FormationDto toDto(Formation formation) {
        return new FormationDto(formation.label(), formation.slots().stream().map(FieldPosition::name).toList());
    }

    private GameStatus statusOf(Game game) {
        if (game.isCompleted()) {
            return GameStatus.COMPLETED;
        }
        return game.isDraftComplete() ? GameStatus.READY_TO_SIMULATE : GameStatus.DRAFTING;
    }

    private Double averageRating(Game game) {
        return game.picks().isEmpty() ? null : game.picks().values().stream()
                .mapToInt(PickedPlayer::rating)
                .average()
                .orElseThrow();
    }

    private List<GameView.SlotView> slotsOf(Game game) {
        List<FieldPosition> positions = game.formation().slots();
        return IntStream.range(0, positions.size())
                .mapToObj(index -> new GameView.SlotView(
                        index,
                        positions.get(index).name(),
                        positions.get(index).line(),
                        pickedView(game.picks().get(index))))
                .toList();
    }

    private GameView.PickedPlayerView pickedView(PickedPlayer pick) {
        return pick == null ? null
                : new GameView.PickedPlayerView(pick.playerId(), pick.name(), pick.rating(), pick.teamName(),
                pick.seasonLabel(), pick.playablePositions().stream().sorted().map(FieldPosition::name).toList());
    }

    private GameView.DrawView drawOf(Game game) {
        TeamSeason teamSeason = catalog.find(game.currentDraw())
                .orElseThrow(() -> new IllegalStateException("Drawn team season missing from catalog"));
        List<GameView.DrawPlayerView> players = eligibility.draftablePlayers(teamSeason).stream()
                .sorted(Comparator.comparingInt(SeasonPlayer::rating).reversed())
                .map(player -> new GameView.DrawPlayerView(
                        player.playerId(),
                        player.name(),
                        player.rating(),
                        player.ratingLine(),
                        player.playablePositions().stream().sorted().map(FieldPosition::name).toList(),
                        eligibility.eligibleSlots(game, player),
                        game.hasPicked(player.playerId())))
                .toList();
        return new GameView.DrawView(teamSeason.teamId(), teamSeason.teamName(), teamSeason.country(),
                teamSeason.seasonLabel(), players);
    }
}
