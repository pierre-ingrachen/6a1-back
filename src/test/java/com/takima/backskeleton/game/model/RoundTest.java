package com.takima.backskeleton.game.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundTest {

    @Test
    void leaguePhaseAndPlayoffsCountAsGroupStage() {
        assertThat(Round.fromDatabase("Phase de ligue", false)).isEqualTo(Round.GROUP_STAGE);
        assertThat(Round.fromDatabase("Barrages", false)).isEqualTo(Round.GROUP_STAGE);
        assertThat(Round.fromDatabase("Phase de groupes", false)).isEqualTo(Round.GROUP_STAGE);
    }

    @Test
    void winnerOverridesTheLastRoundPlayed() {
        assertThat(Round.fromDatabase("", true)).isEqualTo(Round.WINNER);
    }

    @Test
    void beatenFinalistStillPlayedTheFinal() {
        Round beatenFinalist = Round.fromDatabase("Finale", false);

        assertThat(beatenFinalist.hasPlayed(Round.FINAL)).isTrue();
        assertThat(Round.WINNER.hasPlayed(Round.FINAL)).isTrue();
        assertThat(Round.SEMI_FINAL.hasPlayed(Round.FINAL)).isFalse();
    }

    @Test
    void unknownRoundIsRejected() {
        assertThatThrownBy(() -> Round.fromDatabase("Tour préliminaire", false)).isInstanceOf(IllegalArgumentException.class);
    }
}
