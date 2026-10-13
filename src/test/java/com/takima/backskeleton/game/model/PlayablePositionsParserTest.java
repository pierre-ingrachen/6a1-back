package com.takima.backskeleton.game.model;

import org.junit.jupiter.api.Test;

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
    void leftSidedDefenderPlaysCentreAndLeftBack() {
        assertThat(PlayablePositionsParser.parse("D(CL)")).containsExactlyInAnyOrder(CB, LB);
    }

    @Test
    void multiplePositionsAreMerged() {
        assertThat(PlayablePositionsParser.parse("D(R),M(R)")).containsExactlyInAnyOrder(RB, RM);
    }

    @Test
    void defensiveMidfielderIsNotACentralMidfielder() {
        assertThat(PlayablePositionsParser.parse("DMC")).containsExactlyInAnyOrder(DM);
        assertThat(PlayablePositionsParser.parse("M(C)")).containsExactlyInAnyOrder(CM);
    }

    @Test
    void forwardIsAStrikerOnly() {
        assertThat(PlayablePositionsParser.parse("FW")).containsExactlyInAnyOrder(ST);
    }

    @Test
    void wideMidfielderIsNotAWinger() {
        assertThat(PlayablePositionsParser.parse("M(L)")).containsExactlyInAnyOrder(LM);
        assertThat(PlayablePositionsParser.parse("M(R)")).containsExactlyInAnyOrder(RM);
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
    void centralMidfielderWithSidesCoversWideMidfield() {
        assertThat(PlayablePositionsParser.parse("M(CL)")).containsExactlyInAnyOrder(CM, LM);
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

    @Test
    void midfielderWhoAlsoPlaysForwardIsAttackingNotCentreOrWide() {
        assertThat(PlayablePositionsParser.parse("M(C),FW")).containsExactlyInAnyOrder(AM, ST);
        assertThat(PlayablePositionsParser.parse("M(L),FW")).containsExactlyInAnyOrder(LW, ST);
        assertThat(PlayablePositionsParser.parse("M(R),FW")).containsExactlyInAnyOrder(RW, ST);
        assertThat(PlayablePositionsParser.parse("FW,M(CLR)")).containsExactlyInAnyOrder(AM, LW, RW, ST);
    }

    @Test
    void midfielderWithoutForwardKeepsCentreAndWideMidfield() {
        assertThat(PlayablePositionsParser.parse("M(CLR)")).containsExactlyInAnyOrder(CM, LM, RM);
    }
}
