package com.takima.backskeleton.game.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormationTest {

    @ParameterizedTest
    @EnumSource(Formation.class)
    void everyFormationHasElevenSlotsWithOneGoalkeeper(Formation formation) {
        assertThat(formation.slots()).hasSize(Formation.SQUAD_SIZE);
        assertThat(formation.slots()).filteredOn(position -> position.line() == Line.GOALKEEPER).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(Formation.class)
    void everyFormationHasAtMostOneOfEachWinger(Formation formation) {
        assertThat(formation.slots().stream().filter(position -> position == FieldPosition.LW).count()).isLessThanOrEqualTo(1);
        assertThat(formation.slots().stream().filter(position -> position == FieldPosition.RW).count()).isLessThanOrEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Formation.class)
    void everyFormationLabelCountsTenOutfieldPlayers(Formation formation) {
        int outfieldPlayers = Arrays.stream(formation.label().split("-")).mapToInt(Integer::parseInt).sum();
        assertThat(outfieldPlayers).isEqualTo(Formation.SQUAD_SIZE - 1);
    }

    @Test
    void everyFormationHasAUniqueLabel() {
        assertThat(Arrays.stream(Formation.values()).map(Formation::label)).doesNotHaveDuplicates();
    }

    @Test
    void diamondFormationIsFoundByLabel() {
        assertThat(Formation.fromLabel("4-1-2-1-2")).isEqualTo(Formation.FOUR_ONE_TWO_ONE_TWO);
    }

    @Test
    void fourThreeThreeHasOneLeftWinger() {
        assertThat(Formation.FOUR_THREE_THREE.slots()).containsOnlyOnce(FieldPosition.LW);
    }

    @Test
    void formationIsFoundByLabel() {
        assertThat(Formation.fromLabel("4-2-3-1")).isEqualTo(Formation.FOUR_TWO_THREE_ONE);
    }

    @Test
    void unknownLabelIsRejected() {
        assertThatThrownBy(() -> Formation.fromLabel("9-0-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
