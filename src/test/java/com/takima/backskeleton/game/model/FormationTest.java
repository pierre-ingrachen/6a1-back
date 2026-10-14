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
    void labelDefenceNumberMatchesDefenders(Formation formation) {
        int defendersInLabel = Integer.parseInt(formation.label().split("-")[0]);

        assertThat(formation.slots()).filteredOn(position -> position.line() == Line.DEFENDER).hasSize(defendersInLabel);
    }

    @ParameterizedTest
    @EnumSource(Formation.class)
    void labelNumbersAddUpToTenOutfieldPlayers(Formation formation) {
        int outfieldPlayersInLabel = Arrays.stream(formation.label().split("-")).mapToInt(Integer::parseInt).sum();

        assertThat(outfieldPlayersInLabel).isEqualTo(Formation.SQUAD_SIZE - 1);
    }

    @Test
    void everyFormationLabelIsUnique() {
        assertThat(Arrays.stream(Formation.values()).map(Formation::label)).doesNotHaveDuplicates();
    }

    @Test
    void formationsAreSortedByNumberOfDefenders() {
        assertThat(Arrays.stream(Formation.values()).map(formation -> Integer.parseInt(formation.label().split("-")[0])))
                .isSortedAccordingTo((first, second) -> orderOfDefenders(first) - orderOfDefenders(second));
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

    private static int orderOfDefenders(int defenders) {
        return switch (defenders) {
            case 4 -> 0;
            case 3 -> 1;
            default -> 2;
        };
    }
}
