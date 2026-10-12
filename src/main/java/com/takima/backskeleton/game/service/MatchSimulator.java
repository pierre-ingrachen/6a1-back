package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.MatchOutcome;
import com.takima.backskeleton.game.model.PenaltyShootout;
import com.takima.backskeleton.game.model.TeamStrength;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MatchSimulator {
    private static final int REGULAR_PENALTY_KICKS = 5;

    public MatchOutcome playGroupMatch(TeamStrength teamA, TeamStrength teamB, Random random) {
        return MatchOutcome.regular(
                poisson(expectedGoals(teamA, teamB), random),
                poisson(expectedGoals(teamB, teamA), random));
    }

    public MatchOutcome playKnockoutMatch(TeamStrength teamA, TeamStrength teamB, Random random) {
        double expectedA = expectedGoals(teamA, teamB);
        double expectedB = expectedGoals(teamB, teamA);
        int goalsA = poisson(expectedA, random);
        int goalsB = poisson(expectedB, random);
        if (goalsA != goalsB) {
            return MatchOutcome.regular(goalsA, goalsB);
        }
        goalsA += poisson(expectedA * GameBalance.EXTRA_TIME_GOAL_SHARE, random);
        goalsB += poisson(expectedB * GameBalance.EXTRA_TIME_GOAL_SHARE, random);
        PenaltyShootout shootout = goalsA == goalsB ? shootout(random) : null;
        return new MatchOutcome(goalsA, goalsB, true, shootout);
    }

    private double expectedGoals(TeamStrength attacking, TeamStrength defending) {
        return GameBalance.BASE_GOALS * Math.exp(GameBalance.GOAL_SENSITIVITY * (attacking.attack() - defending.defense()));
    }

    private PenaltyShootout shootout(Random random) {
        int scoredA = 0;
        int scoredB = 0;
        for (int kick = 0; kick < REGULAR_PENALTY_KICKS; kick++) {
            scoredA += penaltyScored(random);
            scoredB += penaltyScored(random);
        }
        while (scoredA == scoredB) {
            scoredA += penaltyScored(random);
            scoredB += penaltyScored(random);
        }
        return new PenaltyShootout(scoredA, scoredB);
    }

    private int penaltyScored(Random random) {
        return random.nextDouble() < GameBalance.PENALTY_SCORING_PROBABILITY ? 1 : 0;
    }

    private int poisson(double expected, Random random) {
        double threshold = Math.exp(-expected);
        int goals = 0;
        double product = random.nextDouble();
        while (product > threshold) {
            goals++;
            product *= random.nextDouble();
        }
        return goals;
    }
}
