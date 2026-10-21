package com.takima.backskeleton.game.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoundGroupStageLabelTest {
    @ParameterizedTest
    @ValueSource(strings = {"Phase de groupes", "Phase de ligue", "Barrages", " Barrages "})
    void groupStageLabelsAreRecognized(String label) {
        assertThat(Round.isGroupStageLabel(label)).isTrue();
        assertThat(Round.fromDatabase(label, false)).isEqualTo(Round.GROUP_STAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Huitièmes de finale", "Quarts de finale", "Demi-finales", "Finale", ""})
    void otherLabelsAreNotGroupStage(String label) {
        assertThat(Round.isGroupStageLabel(label)).isFalse();
    }

    @Test
    void nullLabelIsNotGroupStage() {
        assertThat(Round.isGroupStageLabel(null)).isFalse();
    }
}
