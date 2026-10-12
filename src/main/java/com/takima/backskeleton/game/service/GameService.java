package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dao.GameRepository;
import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidRequestException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Service
public class GameService {
    private static final long SIMULATION_SEED_SALT = 0x9E3779B97F4A7C15L;

    private final GameRepository gameRepository;
    private final DraftService draftService;
    private final TeamStrengthService teamStrengthService;
    private final CompetitionSimulator competitionSimulator;
    private final LongSupplier seedSupplier;

    @Autowired
    public GameService(GameRepository gameRepository, DraftService draftService,
                       TeamStrengthService teamStrengthService, CompetitionSimulator competitionSimulator) {
        this(gameRepository, draftService, teamStrengthService, competitionSimulator,
                () -> ThreadLocalRandom.current().nextLong());
    }

    public GameService(GameRepository gameRepository, DraftService draftService, TeamStrengthService teamStrengthService,
                       CompetitionSimulator competitionSimulator, LongSupplier seedSupplier) {
        this.gameRepository = gameRepository;
        this.draftService = draftService;
        this.teamStrengthService = teamStrengthService;
        this.competitionSimulator = competitionSimulator;
        this.seedSupplier = seedSupplier;
    }

    public Game create(String formationLabel) {
        Formation formation = parseFormation(formationLabel);
        return gameRepository.save(draftService.start(formation, seedSupplier.getAsLong()));
    }

    public Game get(UUID gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public synchronized Game pick(UUID gameId, Integer playerId, Integer slotIndex) {
        if (playerId == null || slotIndex == null) {
            throw new InvalidRequestException("playerId and slotIndex are required");
        }
        return gameRepository.save(draftService.pick(get(gameId), playerId, slotIndex));
    }

    public synchronized Game redraw(UUID gameId, RedrawType type) {
        if (type == null) {
            throw new InvalidRequestException("type is required");
        }
        return gameRepository.save(draftService.redraw(get(gameId), type));
    }

    public synchronized Game move(UUID gameId, Integer fromSlot, Integer toSlot) {
        if (fromSlot == null || toSlot == null) {
            throw new InvalidRequestException("fromSlot and toSlot are required");
        }
        return gameRepository.save(draftService.move(get(gameId), fromSlot, toSlot));
    }

    public synchronized Game simulate(UUID gameId) {
        Game game = get(gameId);
        if (!game.isDraftComplete()) {
            throw new DraftIncompleteException(gameId);
        }
        if (game.isCompleted()) {
            return game;
        }
        CompetitionResult result = competitionSimulator.simulate(
                teamStrengthService.ofSquad(game.formation(), game.picks()),
                game.seed() ^ SIMULATION_SEED_SALT);
        return gameRepository.save(game.withResult(result));
    }

    private Formation parseFormation(String label) {
        if (label == null) {
            throw new InvalidRequestException("formation is required");
        }
        try {
            return Formation.fromLabel(label);
        } catch (IllegalArgumentException exception) {
            throw new UnknownFormationException(label);
        }
    }
}
