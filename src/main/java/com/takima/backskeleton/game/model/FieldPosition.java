package com.takima.backskeleton.game.model;

public enum FieldPosition {
    GK(Line.GOALKEEPER),
    LB(Line.DEFENDER),
    CB(Line.DEFENDER),
    RB(Line.DEFENDER),
    DM(Line.MIDFIELDER),
    CM(Line.MIDFIELDER),
    LM(Line.MIDFIELDER),
    RM(Line.MIDFIELDER),
    AM(Line.MIDFIELDER),
    LW(Line.ATTACKER),
    RW(Line.ATTACKER),
    ST(Line.ATTACKER);

    private final Line line;

    FieldPosition(Line line) {
        this.line = line;
    }

    public Line line() {
        return line;
    }
}
