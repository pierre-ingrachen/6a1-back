package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.Line;
import com.takima.backskeleton.game.model.PlayablePositionsParser;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.model.TeamSeasonKey;
import com.takima.backskeleton.game.service.GameBalance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class JdbcTeamSeasonSource implements TeamSeasonSource {
    private static final String QUERY = """
            SELECT s.annee_debut, sa.libelle, s.equipe_id, e.nom AS equipe_nom, e.pays,
                   p.tour_elimination, p.vainqueur,
                   s.joueur_id, j.nom AS joueur_nom, s.note, s.poste_note, s.postes_occupes, s.minutes
            FROM stats_joueur_saison s
            JOIN joueur j ON j.joueur_id = s.joueur_id
            JOIN equipe e ON e.equipe_id = s.equipe_id
            JOIN saison sa ON sa.annee_debut = s.annee_debut
            JOIN parcours_equipe_saison p ON p.annee_debut = s.annee_debut AND p.equipe_id = s.equipe_id
            WHERE s.minutes >= ? AND s.note IS NOT NULL AND s.postes_occupes IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcTeamSeasonSource(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TeamSeason> loadAll() {
        List<Row> rows = jdbcTemplate.query(QUERY, this::mapRow, GameBalance.MIN_OPPONENT_MINUTES);
        return rows.stream()
                .collect(Collectors.groupingBy(Row::key, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toTeamSeason)
                .sorted(Comparator.comparingInt(TeamSeason::startYear).thenComparingInt(TeamSeason::teamId))
                .toList();
    }

    private TeamSeason toTeamSeason(List<Row> teamRows) {
        Row first = teamRows.get(0);
        List<SeasonPlayer> players = teamRows.stream()
                .map(Row::player)
                .sorted(Comparator.comparingInt(SeasonPlayer::rating).reversed().thenComparingInt(SeasonPlayer::playerId))
                .toList();
        return new TeamSeason(first.teamId(), first.teamName(), first.country(), first.startYear(),
                first.seasonLabel(), first.roundReached(), players);
    }

    private Row mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        SeasonPlayer player = new SeasonPlayer(
                resultSet.getInt("joueur_id"),
                resultSet.getString("joueur_nom"),
                resultSet.getInt("note"),
                Line.fromDatabase(resultSet.getString("poste_note")),
                PlayablePositionsParser.parse(resultSet.getString("postes_occupes")),
                resultSet.getInt("minutes"));
        return new Row(
                resultSet.getInt("equipe_id"),
                resultSet.getString("equipe_nom"),
                resultSet.getString("pays"),
                resultSet.getInt("annee_debut"),
                resultSet.getString("libelle"),
                Round.fromDatabase(resultSet.getString("tour_elimination") == null ? "" : resultSet.getString("tour_elimination"),
                        resultSet.getBoolean("vainqueur")),
                player);
    }

    private record Row(int teamId, String teamName, String country, int startYear, String seasonLabel,
                       Round roundReached, SeasonPlayer player) {
        TeamSeasonKey key() {
            return new TeamSeasonKey(teamId, startYear);
        }
    }
}
