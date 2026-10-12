package com.takima.backskeleton.game.support;

import com.takima.backskeleton.game.dao.TeamSeasonSource;
import com.takima.backskeleton.game.model.Line;
import com.takima.backskeleton.game.model.PlayablePositionsParser;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamSeasonKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TsvTeamSeasonSource implements TeamSeasonSource {
    private static final String RESOURCE = "/game/team-seasons.tsv";

    @Override
    public List<TeamSeason> loadAll() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream(RESOURCE), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\t"))
                    .collect(Collectors.groupingBy(
                            columns -> new TeamSeasonKey(Integer.parseInt(columns[2]), Integer.parseInt(columns[0])),
                            LinkedHashMap::new,
                            Collectors.toList()))
                    .values().stream()
                    .map(this::toTeamSeason)
                    .sorted(Comparator.comparingInt(TeamSeason::startYear).thenComparingInt(TeamSeason::teamId))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private TeamSeason toTeamSeason(List<String[]> rows) {
        String[] first = rows.get(0);
        List<SeasonPlayer> players = rows.stream()
                .map(columns -> new SeasonPlayer(
                        Integer.parseInt(columns[6]),
                        columns[7],
                        Integer.parseInt(columns[8]),
                        Line.valueOf(columns[9]),
                        PlayablePositionsParser.parse(columns[10]),
                        Integer.parseInt(columns[11])))
                .toList();
        return new TeamSeason(Integer.parseInt(first[2]), first[3], first[4], Integer.parseInt(first[0]),
                first[1], Round.valueOf(first[5]), players);
    }
}
