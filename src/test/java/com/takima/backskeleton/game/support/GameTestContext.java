package com.takima.backskeleton.game.support;

import com.takima.backskeleton.game.dao.GameRepository;
import com.takima.backskeleton.game.dao.InMemoryGameRepository;
import com.takima.backskeleton.game.service.CompetitionSimulator;
import com.takima.backskeleton.game.service.DraftEligibility;
import com.takima.backskeleton.game.service.DraftService;
import com.takima.backskeleton.game.service.GameLocks;
import com.takima.backskeleton.game.service.GameService;
import com.takima.backskeleton.game.service.MatchSimulator;
import com.takima.backskeleton.game.service.TeamDrawService;
import com.takima.backskeleton.game.service.TeamSeasonCatalog;
import com.takima.backskeleton.game.service.TeamStrengthService;

import java.util.concurrent.atomic.AtomicLong;

public final class GameTestContext {
    private static final TeamStrengthService STRENGTH_SERVICE = new TeamStrengthService();
    private static final TeamSeasonCatalog CATALOG = new TeamSeasonCatalog(new TsvTeamSeasonSource(), STRENGTH_SERVICE);
    private static final DraftEligibility ELIGIBILITY = new DraftEligibility();
    private static final MatchSimulator MATCH_SIMULATOR = new MatchSimulator();

    private static final int DEFAULT_MAX_ACTIVE_GAMES = 1000;

    private GameTestContext() {
    }

    public static TeamSeasonCatalog catalog() {
        return CATALOG;
    }

    public static TeamStrengthService strengthService() {
        return STRENGTH_SERVICE;
    }

    public static DraftEligibility eligibility() {
        return ELIGIBILITY;
    }

    public static MatchSimulator matchSimulator() {
        return MATCH_SIMULATOR;
    }

    public static TeamDrawService drawService() {
        return new TeamDrawService(CATALOG, ELIGIBILITY);
    }

    public static DraftService draftService() {
        return new DraftService(CATALOG, drawService(), ELIGIBILITY);
    }

    public static CompetitionSimulator competitionSimulator() {
        return new CompetitionSimulator(CATALOG, MATCH_SIMULATOR);
    }

    public static GameService gameService(long firstSeed) {
        return gameService(firstSeed, new InMemoryGameRepository());
    }

    public static GameService gameService(long firstSeed, GameRepository gameRepository) {
        return gameService(firstSeed, gameRepository, new GameLocks());
    }

    public static GameService gameService(long firstSeed, GameRepository gameRepository, GameLocks gameLocks) {
        return gameService(firstSeed, gameRepository, gameLocks, DEFAULT_MAX_ACTIVE_GAMES);
    }

    public static GameService gameService(long firstSeed, GameRepository gameRepository, int maxActiveGames) {
        return gameService(firstSeed, gameRepository, new GameLocks(), maxActiveGames);
    }

    public static GameService gameService(long firstSeed, GameRepository gameRepository, GameLocks gameLocks,
                                          int maxActiveGames) {
        AtomicLong seeds = new AtomicLong(firstSeed);
        return new GameService(gameRepository, draftService(), STRENGTH_SERVICE,
                competitionSimulator(), gameLocks, maxActiveGames, seeds::getAndIncrement);
    }
}
