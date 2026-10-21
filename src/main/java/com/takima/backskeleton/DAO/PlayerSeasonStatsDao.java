package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.PlayerAppearance;
import com.takima.backskeleton.models.PlayerSeasonStats;
import com.takima.backskeleton.models.Stat;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlayerSeasonStatsDao {
    private static final Set<String> DERIVED_COLUMNS = Set.of("titularisations", "passes_reussies");

    private static final String STORED_STAT_COLUMNS = Stat.columns().stream()
            .filter(column -> !DERIVED_COLUMNS.contains(column))
            .distinct()
            .map(column -> "s." + column)
            .collect(Collectors.joining(", "));

    private static final String FIND_BY_SEASON = """
            SELECT s.joueur_id, s.note, s.note_moyenne, %s,
                   s.matchs_joues - s.entrees_en_jeu AS titularisations,
                   s.passes_courtes_reussies + s.passes_longues_reussies AS passes_reussies,
                   j.nom AS joueur_nom, j.poste, j.taille_cm, j.poids_kg,
                   e.nom AS equipe_nom,
                   p.tour_elimination
            FROM stats_joueur_saison s
            JOIN joueur j ON j.joueur_id = s.joueur_id
            JOIN equipe e ON e.equipe_id = s.equipe_id
            LEFT JOIN parcours_equipe_saison p ON p.annee_debut = s.annee_debut AND p.equipe_id = s.equipe_id
            WHERE s.annee_debut = :season
            """.formatted(STORED_STAT_COLUMNS);

    private static final String FIND_ALL_APPEARANCES = """
            SELECT j.joueur_id, j.nom AS joueur_nom, j.poste, s.annee_debut, e.nom AS equipe_nom
            FROM stats_joueur_saison s
            JOIN joueur j ON j.joueur_id = s.joueur_id
            JOIN equipe e ON e.equipe_id = s.equipe_id
            ORDER BY s.annee_debut DESC, s.matchs_joues DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PlayerSeasonStatsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlayerSeasonStats> findBySeason(Short season) {
        return jdbcTemplate.queryForList(FIND_BY_SEASON, Map.of("season", season)).stream()
                .map(this::toPlayerSeasonStats)
                .toList();
    }

    public List<PlayerAppearance> findAllAppearances() {
        return jdbcTemplate.queryForList(FIND_ALL_APPEARANCES, Map.of()).stream()
                .map(row -> new PlayerAppearance(
                        ((Number) row.get("joueur_id")).intValue(),
                        (String) row.get("joueur_nom"),
                        (String) row.get("poste"),
                        toShort(row.get("annee_debut")),
                        (String) row.get("equipe_nom")))
                .toList();
    }

    private PlayerSeasonStats toPlayerSeasonStats(Map<String, Object> row) {
        double[] values = PlayerSeasonStats.emptyValues();
        Stat.columns().stream()
                .filter(column -> row.get(column) != null)
                .forEach(column -> values[Stat.columnIndex(column)] = ((Number) row.get(column)).doubleValue());
        return new PlayerSeasonStats(
                ((Number) row.get("joueur_id")).intValue(),
                (String) row.get("joueur_nom"),
                (String) row.get("poste"),
                toShort(row.get("taille_cm")),
                toShort(row.get("poids_kg")),
                (String) row.get("equipe_nom"),
                (String) row.get("tour_elimination"),
                toShort(row.get("note")),
                toDouble(row.get("note_moyenne")),
                values
        );
    }

    private Short toShort(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
