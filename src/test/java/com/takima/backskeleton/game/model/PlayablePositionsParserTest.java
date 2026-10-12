package com.takima.backskeleton.game.model;

import org.junit.jupiter.api.Test;

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
import static org.assertj.core.api.Assertions.assertThat;

class PlayablePositionsParserTest {

    @Test
    void goalkeeperPlaysInGoalOnly() {
        assertThat(PlayablePositionsParser.parse("GK")).containsExactlyInAnyOrder(GK);
    }

    @Test
    void centreBackPlaysCentreOfDefenceOnly() {
        assertThat(PlayablePositionsParser.parse("D(C)")).containsExactlyInAnyOrder(CB);
    }

    @Test
    void leftSidedDefenderAlsoPlaysWingBack() {
        assertThat(PlayablePositionsParser.parse("D(CL)")).containsExactlyInAnyOrder(CB, LB, LWB);
    }

    @Test
    void multiplePositionsAreMerged() {
        assertThat(PlayablePositionsParser.parse("D(R),M(R)")).containsExactlyInAnyOrder(RB, RWB, RW);
    }

    @Test
    void defensiveMidfielderIsACentralMidfielder() {
        assertThat(PlayablePositionsParser.parse("DMC")).containsExactlyInAnyOrder(CM);
    }

    @Test
    void forwardIsAStrikerOnly() {
        assertThat(PlayablePositionsParser.parse("FW")).containsExactlyInAnyOrder(ST);
    }

    @Test
    void wideMidfielderIsAWinger() {
        assertThat(PlayablePositionsParser.parse("M(L)")).containsExactlyInAnyOrder(LW, LWB);
        assertThat(PlayablePositionsParser.parse("M(R)")).containsExactlyInAnyOrder(RW, RWB);
    }

    @Test
    void leftAttackingMidfielderIsALeftWinger() {
        assertThat(PlayablePositionsParser.parse("AM(L)")).containsExactlyInAnyOrder(LW);
    }

    @Test
    void attackingMidfielderAcrossTheLineCoversCentreAndWings() {
        assertThat(PlayablePositionsParser.parse("AM(CLR),FW")).containsExactlyInAnyOrder(AM, LW, RW, ST);
    }

    @Test
    void centralMidfielderWithSidesCoversWidePositions() {
        assertThat(PlayablePositionsParser.parse("M(CL)")).containsExactlyInAnyOrder(CM, LW, LWB);
    }

    @Test
    void legacyPositionNamesAreSupported() {
        assertThat(PlayablePositionsParser.parse("Goalkeeper")).containsExactlyInAnyOrder(GK);
        assertThat(PlayablePositionsParser.parse("Forward")).containsExactlyInAnyOrder(ST);
        assertThat(PlayablePositionsParser.parse("Defender")).contains(CB, LB, RB);
    }

    @Test
    void unknownTokensAreIgnored() {
        assertThat(PlayablePositionsParser.parse("XYZ,GK")).containsExactlyInAnyOrder(GK);
    }
}
