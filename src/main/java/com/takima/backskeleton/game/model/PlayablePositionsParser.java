package com.takima.backskeleton.game.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public final class PlayablePositionsParser {
    private static final Pattern TOKEN = Pattern.compile("^(GK|DMC|FW|AM|D|M)(?:\\(([CLR]+)\\))?$");

    private PlayablePositionsParser() {
    }

    public static Set<FieldPosition> parse(String occupiedPositions) {
        return Arrays.stream(occupiedPositions.split(","))
                .map(String::trim)
                .map(PlayablePositionsParser::normalizeLegacyName)
                .map(TOKEN::matcher)
                .filter(Matcher::matches)
                .flatMap(matcher -> positionsOf(matcher.group(1), matcher.group(2)).stream())
                .collect(() -> EnumSet.noneOf(FieldPosition.class), Set::add, Set::addAll);
    }

    private static String normalizeLegacyName(String token) {
        return switch (token) {
            case "Goalkeeper" -> "GK";
            case "Defender" -> "D(CLR)";
            case "Midfielder" -> "M(CLR)";
            case "Forward" -> "FW";
            default -> token;
        };
    }

    private static Set<FieldPosition> positionsOf(String code, String sides) {
        Set<FieldPosition> positions = EnumSet.noneOf(FieldPosition.class);
        String safeSides = sides == null ? "" : sides;
        boolean centre = safeSides.contains("C");
        boolean left = safeSides.contains("L");
        boolean right = safeSides.contains("R");
        switch (code) {
            case "GK" -> positions.add(GK);
            case "DMC" -> positions.add(CM);
            case "FW" -> positions.add(ST);
            case "AM" -> {
                addIf(centre, positions, AM);
                addIf(left, positions, LW);
                addIf(right, positions, RW);
            }
            case "D" -> {
                addIf(centre, positions, CB);
                addIf(left, positions, LB, LWB);
                addIf(right, positions, RB, RWB);
            }
            case "M" -> {
                addIf(centre, positions, CM);
                addIf(left, positions, LW, LWB);
                addIf(right, positions, RW, RWB);
            }
            default -> {
            }
        }
        return positions;
    }

    private static void addIf(boolean condition, Set<FieldPosition> target, FieldPosition... positions) {
        if (condition) {
            target.addAll(Arrays.asList(positions));
        }
    }
}
