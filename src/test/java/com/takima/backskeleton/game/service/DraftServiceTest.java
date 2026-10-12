package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.DraftCompletedException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.model.FieldPosition;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.PickedPlayer;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamStrength;
import com.takima.backskeleton.game.support.DraftBot;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DraftServiceTest {
    private static final long SEED = 2024L;

    private final DraftService draftService = GameTestContext.draftService();
    private final DraftEligibility eligibility = GameTestContext.eligibility();

    @Test
    void startDrawsAFirstTeamAndGrantsThreeRedraws() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);

        assertThat(game.currentDraw()).isNotNull();
        assertThat(game.redrawsLeft()).isEqualTo(3);
        assertThat(game.picks()).isEmpty();
        assertThat(game.picksRemaining()).isEqualTo(11);
    }

    @Test
    void sameSeedDrawsSameFirstTeam() {
        Game first = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        Game second = draftService.start(Formation.FOUR_FOUR_TWO, SEED);

        assertThat(first.currentDraw()).isEqualTo(second.currentDraw());
    }

    @Test
    void pickFillsTheChosenSlotAndDrawsTheNextTeam() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        SeasonPlayer player = firstPickable(game);
        int slot = eligibility.eligibleSlots(game, player).get(0);

        Game updated = draftService.pick(game, player.playerId(), slot);

        assertThat(updated.picks()).containsOnlyKeys(slot);
        assertThat(updated.picks().get(slot).playerId()).isEqualTo(player.playerId());
        assertThat(updated.currentDraw()).isNotEqualTo(game.currentDraw());
        assertThat(updated.picksRemaining()).isEqualTo(10);
    }

    @Test
    void pickRejectsPlayerOutsideTheCurrentDraw() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);

        assertThatThrownBy(() -> draftService.pick(game, -1, 0)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void pickRejectsPlayerWhoCannotPlayTheSlot() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        SeasonPlayer player = firstPickable(game);
        int incompatibleSlot = game.openSlotIndexes().stream()
                .filter(index -> !player.canPlay(game.formation().slots().get(index)))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> draftService.pick(game, player.playerId(), incompatibleSlot))
                .isInstanceOf(InvalidPickException.class);
    }

    @Test
    void pickRejectsUnknownSlot() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        SeasonPlayer player = firstPickable(game);

        assertThatThrownBy(() -> draftService.pick(game, player.playerId(), 11)).isInstanceOf(InvalidPickException.class);
        assertThatThrownBy(() -> draftService.pick(game, player.playerId(), -1)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void pickRejectsAlreadyFilledSlot() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        SeasonPlayer player = firstPickable(game);
        int slot = eligibility.eligibleSlots(game, player).get(0);
        Game updated = draftService.pick(game, player.playerId(), slot);
        SeasonPlayer next = firstPickable(updated);

        assertThatThrownBy(() -> draftService.pick(updated, next.playerId(), slot)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void drawNeverOffersAlreadyPickedPlayerAsPickable() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playStarSeeker(draftService.start(Formation.FOUR_FOUR_TWO, SEED), 0);

        long distinctPlayers = finished.picks().values().stream().map(PickedPlayer::playerId).distinct().count();
        assertThat(distinctPlayers).isEqualTo(Formation.SQUAD_SIZE);
    }

    @Test
    void everyDrawOffersAtLeastOnePickablePlayer() {
        Game game = draftService.start(Formation.FOUR_TWO_THREE_ONE, SEED);
        Random random = new Random(SEED);

        while (!game.isDraftComplete()) {
            List<SeasonPlayer> pickable = pickablePlayers(game);
            assertThat(pickable).isNotEmpty();
            SeasonPlayer player = pickable.get(random.nextInt(pickable.size()));
            game = draftService.pick(game, player.playerId(), eligibility.eligibleSlots(game, player).get(0));
        }

        assertThat(game.currentDraw()).isNull();
    }

    @Test
    void draftedSquadRespectsFormationPositions() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playRandomly(draftService.start(Formation.FOUR_THREE_THREE, SEED));

        assertThat(finished.picks()).hasSize(Formation.SQUAD_SIZE);
        finished.picks().forEach((slotIndex, pick) -> {
            FieldPosition position = finished.formation().slots().get(slotIndex);
            assertThat(playerOf(pick.playerId()).canPlay(position)).as(pick.name() + " at " + position).isTrue();
        });
    }

    @Test
    void fourThreeThreeAcceptsOnlyOneLeftWinger() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playRandomly(draftService.start(Formation.FOUR_THREE_THREE, SEED));

        long leftWingers = finished.picks().keySet().stream()
                .filter(slotIndex -> finished.formation().slots().get(slotIndex) == FieldPosition.LW)
                .count();
        assertThat(leftWingers).isEqualTo(1);
    }

    @Test
    void pickIsRejectedOnceDraftIsComplete() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playRandomly(draftService.start(Formation.FOUR_THREE_THREE, SEED));

        assertThatThrownBy(() -> draftService.pick(finished, 1, 0)).isInstanceOf(DraftCompletedException.class);
        assertThatThrownBy(() -> draftService.redraw(finished, RedrawType.TEAM)).isInstanceOf(DraftCompletedException.class);
    }

    @Test
    void seasonRedrawKeepsTheTeamAndChangesTheYear() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);

        Game redrawn = draftService.redraw(game, RedrawType.SEASON);

        assertThat(redrawn.currentDraw().teamId()).isEqualTo(game.currentDraw().teamId());
        assertThat(redrawn.currentDraw().startYear()).isNotEqualTo(game.currentDraw().startYear());
        assertThat(redrawn.redrawsLeft()).isEqualTo(2);
    }

    @Test
    void teamRedrawKeepsTheYearAndChangesTheTeam() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);

        Game redrawn = draftService.redraw(game, RedrawType.TEAM);

        assertThat(redrawn.currentDraw().startYear()).isEqualTo(game.currentDraw().startYear());
        assertThat(redrawn.currentDraw().teamId()).isNotEqualTo(game.currentDraw().teamId());
    }

    @Test
    void redrawNeverReturnsATeamSeasonAlreadySeen() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        Game afterRedraws = game;
        for (int redraw = 0; redraw < Game.INITIAL_REDRAWS; redraw++) {
            afterRedraws = draftService.redraw(afterRedraws, redraw % 2 == 0 ? RedrawType.TEAM : RedrawType.SEASON);
        }

        assertThat(afterRedraws.seenDraws()).hasSize(Game.INITIAL_REDRAWS + 1);
    }

    @Test
    void redrawQuotaIsLimitedToThree() {
        Game game = draftService.start(Formation.FOUR_THREE_THREE, SEED);
        Game exhausted = game;
        for (int redraw = 0; redraw < Game.INITIAL_REDRAWS; redraw++) {
            exhausted = draftService.redraw(exhausted, RedrawType.TEAM);
        }
        Game finalGame = exhausted;

        assertThat(finalGame.redrawsLeft()).isZero();
        assertThatThrownBy(() -> draftService.redraw(finalGame, RedrawType.TEAM)).isInstanceOf(NoRedrawAvailableException.class);
    }

    private SeasonPlayer firstPickable(Game game) {
        return pickablePlayers(game).get(0);
    }

    private List<SeasonPlayer> pickablePlayers(Game game) {
        TeamSeason drawn = draftService.currentTeamSeason(game);
        return eligibility.draftablePlayers(drawn).stream()
                .filter(player -> !eligibility.eligibleSlots(game, player).isEmpty())
                .toList();
    }

    private SeasonPlayer playerOf(int playerId) {
        return GameTestContext.catalog().teamSeasons().stream()
                .flatMap(teamSeason -> teamSeason.players().stream())
                .filter(player -> player.playerId() == playerId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void moveRelocatesAPlayerToAnEmptySlotItCanPlay() {
        Game game = gameWithOnePick(Formation.FOUR_FOUR_TWO);
        int from = game.picks().keySet().iterator().next();
        PickedPlayer moved = game.picks().get(from);
        int target = game.openSlotIndexes().stream()
                .filter(index -> moved.canPlay(game.formation().slots().get(index)))
                .findFirst().orElseThrow();

        Game updated = draftService.move(game, from, target);

        assertThat(updated.picks()).containsOnlyKeys(target);
        assertThat(updated.picks().get(target)).isEqualTo(moved);
        assertThat(updated.picksRemaining()).isEqualTo(game.picksRemaining());
    }

    @Test
    void moveRejectsASlotTheDraftedPlayerCannotPlay() {
        Game game = gameWithOnePick(Formation.FOUR_FOUR_TWO);
        int from = game.picks().keySet().iterator().next();
        PickedPlayer moved = game.picks().get(from);
        int impossible = game.openSlotIndexes().stream()
                .filter(index -> !moved.canPlay(game.formation().slots().get(index)))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> draftService.move(game, from, impossible)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void moveRejectsAnEmptySourceSlot() {
        Game game = draftService.start(Formation.FOUR_FOUR_TWO, SEED);

        assertThatThrownBy(() -> draftService.move(game, 0, 1)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void moveSwapsTwoPlayersWhoCanPlayEachOthersSlot() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playRandomly(draftService.start(Formation.FOUR_FOUR_TWO, SEED));
        int centreBackA = slotOf(finished, FieldPosition.CB, 0);
        int centreBackB = slotOf(finished, FieldPosition.CB, 1);
        PickedPlayer first = finished.picks().get(centreBackA);
        PickedPlayer second = finished.picks().get(centreBackB);

        Game swapped = draftService.move(finished, centreBackA, centreBackB);

        assertThat(swapped.picks().get(centreBackB)).isEqualTo(first);
        assertThat(swapped.picks().get(centreBackA)).isEqualTo(second);
    }

    @Test
    void moveRejectsASwapWhenTheDisplacedPlayerCannotPlayTheSourceSlot() {
        Game finished = new DraftBot(draftService, eligibility, new Random(SEED))
                .playRandomly(draftService.start(Formation.FOUR_FOUR_TWO, SEED));
        int goalkeeper = slotOf(finished, FieldPosition.GK, 0);
        int striker = slotOf(finished, FieldPosition.ST, 0);

        assertThatThrownBy(() -> draftService.move(finished, goalkeeper, striker)).isInstanceOf(InvalidPickException.class);
    }

    @Test
    void moveIsRejectedOnceTheCompetitionIsSimulated() {
        Game simulated = GameTestContext.gameService(3L).create("4-4-2");
        Game game = simulated.withResult(GameTestContext.competitionSimulator()
                .simulate(new TeamStrength(80, 80), 1L));

        assertThatThrownBy(() -> draftService.move(game, 0, 1)).isInstanceOf(DraftCompletedException.class);
    }

    @Test
    void movedDraftStillOffersAPickablePlayer() {
        Game game = gameWithOnePick(Formation.FOUR_TWO_THREE_ONE);
        int from = game.picks().keySet().iterator().next();
        PickedPlayer moved = game.picks().get(from);
        int target = game.openSlotIndexes().stream()
                .filter(index -> moved.canPlay(game.formation().slots().get(index)))
                .findFirst().orElseThrow();

        Game updated = draftService.move(game, from, target);

        assertThat(pickablePlayers(updated)).isNotEmpty();
    }

    private Game gameWithOnePick(Formation formation) {
        Game game = draftService.start(formation, SEED);
        SeasonPlayer player = pickablePlayers(game).stream()
                .filter(candidate -> candidate.playablePositions().size() > 1)
                .findFirst().orElse(pickablePlayers(game).get(0));
        return draftService.pick(game, player.playerId(), eligibility.eligibleSlots(game, player).get(0));
    }

    private int slotOf(Game game, FieldPosition position, int occurrence) {
        return indexesOf(game, position).get(occurrence);
    }

    private List<Integer> indexesOf(Game game, FieldPosition position) {
        return IntStream.range(0, game.formation().slots().size())
                .filter(index -> game.formation().slots().get(index) == position)
                .boxed().toList();
    }
}
