package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameCapacityReachedException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import com.takima.backskeleton.game.dao.InMemoryGameRepository;
import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.support.GameTestContext;
import com.takima.backskeleton.game.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

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
    void simulationRequiresACompleteDraft() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.simulate(created.id())).isInstanceOf(DraftIncompleteException.class);
    }

    @Test
    void redrawIsPersisted() {
        Game created = gameService.create("4-3-3");

        gameService.redraw(created.id(), RedrawType.TEAM);

        assertThat(gameService.get(created.id()).redrawsLeft()).isEqualTo(2);
    }

    @Test
    void completeDraftIsSimulatedAndGameIsRemoved() {
        Game game = completeDraft("4-4-2");

        CompetitionResult result = gameService.simulate(game.id());

        assertThat(result).isNotNull();
        assertThat(result.groupStage().matches()).hasSize(6);
        assertThatThrownBy(() -> gameService.get(game.id())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void simulatingTwiceFailsWithNotFound() {
        Game game = completeDraft("4-4-2");
        gameService.simulate(game.id());

        assertThatThrownBy(() -> gameService.simulate(game.id())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void incompleteSimulationKeepsTheGame() {
        Game created = gameService.create("4-3-3");

        assertThatThrownBy(() -> gameService.simulate(created.id())).isInstanceOf(DraftIncompleteException.class);

        assertThat(gameService.get(created.id())).isEqualTo(created);
    }

    @Test
    void idleGamesAreRemoved() {
        MutableClock clock = new MutableClock();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock));
        Game idle = service.create("4-3-3");
        clock.advance(GameService.GAME_IDLE_TTL.plusMinutes(1));
        Game recent = service.create("4-3-3");

        service.removeIdleGames();

        assertThatThrownBy(() -> service.get(idle.id())).isInstanceOf(GameNotFoundException.class);
        assertThat(service.get(recent.id())).isEqualTo(recent);
    }

    @Test
    void gamesWithoutAnyPickAreRemovedAfterTheShortTtl() {
        MutableClock clock = new MutableClock();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock));
        Game empty = service.create("4-3-3");
        clock.advance(GameService.EMPTY_GAME_IDLE_TTL.plusMinutes(1));

        service.removeIdleGames();

        assertThatThrownBy(() -> service.get(empty.id())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void gamesWithoutAnyPickSurviveBeforeTheShortTtl() {
        MutableClock clock = new MutableClock();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock));
        Game empty = service.create("4-3-3");
        clock.advance(GameService.EMPTY_GAME_IDLE_TTL.minusMinutes(1));

        service.removeIdleGames();

        assertThat(service.get(empty.id())).isEqualTo(empty);
    }

    @Test
    void gamesWithAPickKeepTheLongTtl() {
        MutableClock clock = new MutableClock();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock));
        Game created = service.create("4-3-3");
        SeasonPlayer player = bestPickable(created);
        Game started = service.pick(created.id(), player.playerId(), eligibility.eligibleSlots(created, player).get(0));
        clock.advance(GameService.EMPTY_GAME_IDLE_TTL.plusMinutes(1));

        service.removeIdleGames();

        assertThat(service.get(started.id())).isEqualTo(started);
        clock.advance(GameService.GAME_IDLE_TTL.plusMinutes(1));
        service.removeIdleGames();
        assertThatThrownBy(() -> service.get(started.id())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void emptyGamesPurgeReleasesTheGameLock() {
        MutableClock clock = new MutableClock();
        GameLocks locks = new GameLocks();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock), locks);
        Game game = service.create("4-3-3");
        service.redraw(game.id(), RedrawType.TEAM);
        clock.advance(GameService.EMPTY_GAME_IDLE_TTL.plusMinutes(1));

        service.removeIdleGames();

        assertThat(locks.size()).isZero();
    }

    @Test
    void simulationReleasesTheGameLock() {
        GameLocks locks = new GameLocks();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(), locks);
        Game game = completeDraft(service, "4-4-2");

        service.simulate(game.id());

        assertThat(locks.size()).isZero();
    }

    @Test
    void idleRemovalReleasesTheGameLock() {
        MutableClock clock = new MutableClock();
        GameLocks locks = new GameLocks();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock), locks);
        Game game = service.create("4-3-3");
        service.redraw(game.id(), RedrawType.TEAM);
        clock.advance(GameService.GAME_IDLE_TTL.plusMinutes(1));

        service.removeIdleGames();

        assertThat(locks.size()).isZero();
    }

    @Test
    void operationOnUnknownGameDoesNotLeakALock() {
        GameLocks locks = new GameLocks();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(), locks);

        assertThatThrownBy(() -> service.pick(UUID.randomUUID(), 1, 0)).isInstanceOf(GameNotFoundException.class);

        assertThat(locks.size()).isZero();
    }

    @Test
    void simultaneousPicksOnTheSameGameAreSerialized() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 50; attempt++) {
                Game game = gameService.create("4-3-3");
                SeasonPlayer player = bestPickable(game);
                int slot = eligibility.eligibleSlots(game, player).get(0);
                CyclicBarrier barrier = new CyclicBarrier(2);
                Callable<Boolean> pick = () -> {
                    barrier.await();
                    try {
                        gameService.pick(game.id(), player.playerId(), slot);
                        return true;
                    } catch (InvalidPickException exception) {
                        return false;
                    }
                };

                List<Future<Boolean>> outcomes = executor.invokeAll(List.of(pick, pick));

                assertThat(outcomes.stream().map(GameServiceTest::resultOf).filter(Boolean::booleanValue)).hasSize(1);
                assertThat(gameService.get(game.id()).picks()).hasSize(1);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean resultOf(Future<Boolean> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void creationIsRejectedOnceTheActiveGamesCeilingIsReached() {
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(), 2);
        service.create("4-3-3");
        service.create("4-3-3");

        assertThatThrownBy(() -> service.create("4-3-3")).isInstanceOf(GameCapacityReachedException.class);
    }

    @Test
    void finishedGamesFreeRoomForNewOnes() {
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(), 1);
        Game game = completeDraft(service, "4-4-2");
        service.simulate(game.id());

        assertThat(service.create("4-3-3")).isNotNull();
    }

    @Test
    void idleGamesAreReclaimedWhenTheCeilingIsReached() {
        MutableClock clock = new MutableClock();
        GameService service = GameTestContext.gameService(11L, new InMemoryGameRepository(clock), 1);
        Game idle = service.create("4-3-3");
        clock.advance(GameService.GAME_IDLE_TTL.plusMinutes(1));

        Game fresh = service.create("4-3-3");

        assertThat(service.get(fresh.id())).isEqualTo(fresh);
        assertThatThrownBy(() -> service.get(idle.id())).isInstanceOf(GameNotFoundException.class);
    }

    private Game completeDraft(String formation) {
        return completeDraft(gameService, formation);
    }

    private Game completeDraft(GameService service, String formation) {
        Game game = service.create(formation);
        while (!game.isDraftComplete()) {
            SeasonPlayer player = bestPickable(game);
            game = service.pick(game.id(), player.playerId(), eligibility.eligibleSlots(game, player).get(0));
        }
        return game;
    }

    private SeasonPlayer bestPickable(Game game) {
        return eligibility.draftablePlayers(GameTestContext.catalog().find(game.currentDraw()).orElseThrow()).stream()
                .filter(player -> !eligibility.eligibleSlots(game, player).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    @Test
    void refusedSeasonRedrawKeepsTheRedrawQuotaAndTheDraw() {
        TeamDrawService teamDrawService = GameTestContext.drawService();
        DraftService draftService = GameTestContext.draftService();
        long seed = LongStream.range(0, 500)
                .filter(candidate -> !teamDrawService.canRedraw(draftService.start(Formation.FOUR_THREE_THREE, candidate), RedrawType.SEASON))
                .findFirst().orElseThrow();
        GameService service = GameTestContext.gameService(seed);
        Game created = service.create("4-3-3");

        assertThatThrownBy(() -> service.redraw(created.id(), RedrawType.SEASON)).isInstanceOf(NoRedrawAvailableException.class);

        assertThat(service.get(created.id())).isEqualTo(created);
        assertThat(service.get(created.id()).redrawsLeft()).isEqualTo(Game.INITIAL_REDRAWS);
    }
}
