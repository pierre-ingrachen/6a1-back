package com.takima.backskeleton.game.model;

import java.util.Arrays;
import java.util.List;

import static com.takima.backskeleton.game.model.FieldPosition.AM;
import static com.takima.backskeleton.game.model.FieldPosition.CB;
import static com.takima.backskeleton.game.model.FieldPosition.CM;
import static com.takima.backskeleton.game.model.FieldPosition.DM;
import static com.takima.backskeleton.game.model.FieldPosition.GK;
import static com.takima.backskeleton.game.model.FieldPosition.LB;
import static com.takima.backskeleton.game.model.FieldPosition.LM;
import static com.takima.backskeleton.game.model.FieldPosition.LW;
import static com.takima.backskeleton.game.model.FieldPosition.RB;
import static com.takima.backskeleton.game.model.FieldPosition.RM;
import static com.takima.backskeleton.game.model.FieldPosition.RW;
import static com.takima.backskeleton.game.model.FieldPosition.ST;

public enum Formation {
    FOUR_THREE_THREE("4-3-3", GK, LB, CB, CB, RB, CM, CM, CM, LW, ST, RW),
    FOUR_FOUR_TWO("4-4-2", GK, LB, CB, CB, RB, LM, CM, CM, RM, ST, ST),
    FOUR_TWO_THREE_ONE("4-2-3-1", GK, LB, CB, CB, RB, DM, DM, LW, AM, RW, ST),
    FOUR_ONE_FOUR_ONE("4-1-4-1", GK, LB, CB, CB, RB, DM, LM, CM, CM, RM, ST),
    FOUR_TWO_FOUR("4-2-4", GK, LB, CB, CB, RB, CM, CM, LW, ST, ST, RW),
    FOUR_ONE_TWO_ONE_TWO("4-1-2-1-2", GK, LB, CB, CB, RB, DM, CM, CM, AM, ST, ST),
    THREE_FOUR_THREE("3-4-3", GK, CB, CB, CB, LM, CM, CM, RM, LW, ST, RW),
    THREE_FIVE_TWO("3-5-2", GK, CB, CB, CB, LM, CM, CM, CM, RM, ST, ST),
    THREE_FOUR_TWO_ONE("3-4-2-1", GK, CB, CB, CB, LM, CM, CM, RM, AM, AM, ST),
    FIVE_THREE_TWO("5-3-2", GK, LB, CB, CB, CB, RB, CM, CM, CM, ST, ST),
    FIVE_TWO_THREE("5-2-3", GK, LB, CB, CB, CB, RB, CM, CM, LW, ST, RW),
    FIVE_FOUR_ONE("5-4-1", GK, LB, CB, CB, CB, RB, LM, CM, CM, RM, ST);

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
