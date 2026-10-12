package com.takima.backskeleton.game.model;

import java.util.Arrays;
import java.util.List;

import static com.takima.backskeleton.game.model.FieldPosition.AM;
import static com.takima.backskeleton.game.model.FieldPosition.CB;
import static com.takima.backskeleton.game.model.FieldPosition.CM;
import static com.takima.backskeleton.game.model.FieldPosition.GK;
import static com.takima.backskeleton.game.model.FieldPosition.LB;
import static com.takima.backskeleton.game.model.FieldPosition.LW;
import static com.takima.backskeleton.game.model.FieldPosition.LWB;
import static com.takima.backskeleton.game.model.FieldPosition.RB;
import static com.takima.backskeleton.game.model.FieldPosition.RW;
import static com.takima.backskeleton.game.model.FieldPosition.RWB;
import static com.takima.backskeleton.game.model.FieldPosition.ST;

public enum Formation {
    FOUR_THREE_THREE("4-3-3", GK, LB, CB, CB, RB, CM, CM, CM, LW, ST, RW),
    FOUR_FOUR_TWO("4-4-2", GK, LB, CB, CB, RB, LW, CM, CM, RW, ST, ST),
    FOUR_TWO_THREE_ONE("4-2-3-1", GK, LB, CB, CB, RB, CM, CM, LW, AM, RW, ST),
    THREE_FIVE_TWO("3-5-2", GK, CB, CB, CB, LWB, RWB, CM, CM, CM, ST, ST),
    FIVE_THREE_TWO("5-3-2", GK, LWB, CB, CB, CB, RWB, CM, CM, CM, ST, ST);

    public static final int SQUAD_SIZE = 11;

    private final String label;
    private final List<FieldPosition> slots;

    Formation(String label, FieldPosition... slots) {
        this.label = label;
        this.slots = List.of(slots);
    }

    public String label() {
        return label;
    }

    public List<FieldPosition> slots() {
        return slots;
    }

    public static Formation fromLabel(String label) {
        return Arrays.stream(values())
                .filter(formation -> formation.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown formation " + label));
    }
}
