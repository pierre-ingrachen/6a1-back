package com.takima.backskeleton.game.support;

import com.takima.backskeleton.game.dao.InMemoryGameRepository;
import com.takima.backskeleton.game.service.CompetitionSimulator;
import com.takima.backskeleton.game.service.DraftEligibility;
import com.takima.backskeleton.game.service.DraftService;
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
        AtomicLong seeds = new AtomicLong(firstSeed);
        return new GameService(new InMemoryGameRepository(), draftService(), STRENGTH_SERVICE,
                competitionSimulator(), seeds::getAndIncrement);
    }
}
