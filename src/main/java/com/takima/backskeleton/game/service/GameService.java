package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dao.GameRepository;
import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameCapacityReachedException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.function.LongSupplier;

@Service
public class GameService {
    static final Duration GAME_IDLE_TTL = Duration.ofHours(2);
    private static final long SIMULATION_SEED_SALT = 0x9E3779B97F4A7C15L;

    private final GameRepository gameRepository;
    private final DraftService draftService;
    private final TeamStrengthService teamStrengthService;
    private final CompetitionSimulator competitionSimulator;
    private final GameLocks gameLocks;
    private final int maxActiveGames;
    private final Object creationLock = new Object();
    private final LongSupplier seedSupplier;

    @Autowired
    public GameService(GameRepository gameRepository, DraftService draftService,
                       TeamStrengthService teamStrengthService, CompetitionSimulator competitionSimulator,
                       GameLocks gameLocks, @Value("${game.max-active-games:1000}") int maxActiveGames) {
        this(gameRepository, draftService, teamStrengthService, competitionSimulator, gameLocks, maxActiveGames,
                () -> ThreadLocalRandom.current().nextLong());
    }

    public GameService(GameRepository gameRepository, DraftService draftService, TeamStrengthService teamStrengthService,
                       CompetitionSimulator competitionSimulator, GameLocks gameLocks,
                       int maxActiveGames, LongSupplier seedSupplier) {
        this.gameRepository = gameRepository;
        this.draftService = draftService;
        this.teamStrengthService = teamStrengthService;
        this.competitionSimulator = competitionSimulator;
        this.gameLocks = gameLocks;
        this.maxActiveGames = maxActiveGames;
        this.seedSupplier = seedSupplier;
    }

    public Game create(String formationLabel) {
        Formation formation = parseFormation(formationLabel);
        synchronized (creationLock) {
            requireCapacity();
            return gameRepository.save(draftService.start(formation, seedSupplier.getAsLong()));
        }
    }

    public Game get(UUID gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public Game pick(UUID gameId, int playerId, int slotIndex) {
        return update(gameId, game -> draftService.pick(game, playerId, slotIndex));
    }

    public Game redraw(UUID gameId, RedrawType type) {
        return update(gameId, game -> draftService.redraw(game, type));
    }

    public Game move(UUID gameId, int fromSlot, int toSlot) {
        return update(gameId, game -> draftService.move(game, fromSlot, toSlot));
    }

    public CompetitionResult simulate(UUID gameId) {
        return locked(gameId, () -> {
            Game game = get(gameId);
            if (!game.isDraftComplete()) {
                throw new DraftIncompleteException(gameId);
            }
            CompetitionResult result = competitionSimulator.simulate(
                    teamStrengthService.ofSquad(game.formation(), game.picks()),
                    game.seed() ^ SIMULATION_SEED_SALT);
            gameRepository.deleteById(gameId);
            gameLocks.release(gameId);
            return result;
        });
    }

    public void removeIdleGames() {
        gameRepository.deleteIdleFor(GAME_IDLE_TTL).forEach(gameLocks::release);
    }

    private Game update(UUID gameId, UnaryOperator<Game> change) {
        return locked(gameId, () -> gameRepository.save(change.apply(get(gameId))));
    }

    private <T> T locked(UUID gameId, Supplier<T> action) {
        try {
            return gameLocks.withLock(gameId, action);
        } catch (GameNotFoundException exception) {
            gameLocks.release(gameId);
            throw exception;
        }
    }

    private void requireCapacity() {
        if (gameRepository.count() >= maxActiveGames) {
            removeIdleGames();
        }
        if (gameRepository.count() >= maxActiveGames) {
            throw new GameCapacityReachedException(maxActiveGames);
        }
    }

    private Formation parseFormation(String label) {
        try {
            return Formation.fromLabel(label);
        } catch (IllegalArgumentException exception) {
            throw new UnknownFormationException(label);
        }
    }
}
