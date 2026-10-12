package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidRequestException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {
    private final GameService gameService = GameTestContext.gameService(11L);
    private final DraftEligibility eligibility = GameTestContext.eligibility();

    @Test
    void createdGameIsRetrievable() {
        Game created = gameService.create("4-3-3");

        assertThat(gameService.get(created.id())).isEqualTo(created);
    }

    @Test
    void unknownGameIsNotFound() {
        assertThatThrownBy(() -> gameService.get(UUID.randomUUID())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void unknownFormationIsRejected() {
        assertThatThrownBy(() -> gameService.create("9-9-9")).isInstanceOf(UnknownFormationException.class);
    }

    @Test
    void missingFormationIsRejected() {
        assertThatThrownBy(() -> gameService.create(null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void pickWithoutPlayerIsRejected() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.pick(created.id(), null, 0)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void redrawWithoutTypeIsRejected() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.redraw(created.id(), null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void simulationRequiresACompleteDraft() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.simulate(created.id())).isInstanceOf(DraftIncompleteException.class);
    }

    @Test
    void moveWithoutSlotsIsRejected() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.move(created.id(), null, 1)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void redrawIsPersisted() {
        Game created = gameService.create("4-3-3");

        gameService.redraw(created.id(), RedrawType.TEAM);

        assertThat(gameService.get(created.id()).redrawsLeft()).isEqualTo(2);
    }

    @Test
    void completeDraftCanBeSimulatedAndResultIsStable() {
        Game game = gameService.create("4-4-2");
        while (!game.isDraftComplete()) {
            SeasonPlayer player = bestPickable(game);
            game = gameService.pick(game.id(), player.playerId(), eligibility.eligibleSlots(game, player).get(0));
        }

        Game simulated = gameService.simulate(game.id());
        Game simulatedAgain = gameService.simulate(game.id());

        assertThat(simulated.result()).isNotNull();
        assertThat(simulated.result().groupStage().matches()).hasSize(6);
        assertThat(simulatedAgain.result()).isEqualTo(simulated.result());
    }

    private SeasonPlayer bestPickable(Game game) {
        return eligibility.draftablePlayers(GameTestContext.catalog().find(game.currentDraw()).orElseThrow()).stream()
                .filter(player -> !eligibility.eligibleSlots(game, player).isEmpty())
                .findFirst()
                .orElseThrow();
    }
}
