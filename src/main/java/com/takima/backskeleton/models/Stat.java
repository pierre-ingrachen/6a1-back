package com.takima.backskeleton.models;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.takima.backskeleton.models.StatCategory.AERIAL_DUELS;
import static com.takima.backskeleton.models.StatCategory.CREATION;
import static com.takima.backskeleton.models.StatCategory.CROSSES_AND_SET_PIECES;
import static com.takima.backskeleton.models.StatCategory.DEFENDING;
import static com.takima.backskeleton.models.StatCategory.DISCIPLINE;
import static com.takima.backskeleton.models.StatCategory.DRIBBLING;
import static com.takima.backskeleton.models.StatCategory.GOALKEEPING;
import static com.takima.backskeleton.models.StatCategory.GOALS;
import static com.takima.backskeleton.models.StatCategory.PASSING;
import static com.takima.backskeleton.models.StatCategory.PLAYING_TIME;
import static com.takima.backskeleton.models.StatCategory.SHOTS;

public enum Stat {
    GOALS_SCORED("buts", "Buts", GOALS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    CONVERSION_RATE("Conversion (%)", GOALS, "buts", "tirs", 10),
    PENALTIES_SCORED("penaltys_marques", "Penaltys marqués", GOALS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    PENALTIES_TAKEN("penaltys_tires", "Penaltys tirés", GOALS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    OWN_GOALS("buts_contre_son_camp", "Buts contre son camp", GOALS, Level.MAIN, Direction.LOWER_IS_BETTER),
    GOALS_SIX_YARD_BOX("buts_petite_surface", "Dans les 6 mètres", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_PENALTY_AREA("buts_surface", "Dans la surface", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_OUTSIDE_BOX("buts_hors_surface", "Hors de la surface", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_OPEN_PLAY("buts_jeu_ouvert", "En jeu ouvert", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_COUNTER_ATTACK("buts_contre_attaque", "En contre-attaque", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_SET_PIECE("buts_coup_arrete", "Sur coup de pied arrêté", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_NORMAL_SITUATION("buts_normal", "En situation normale", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_HEADER("buts_tete", "De la tête", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_LEFT_FOOT("buts_pied_gauche", "Du pied gauche", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_RIGHT_FOOT("buts_pied_droit", "Du pied droit", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    GOALS_OTHER_BODY_PART("buts_autre_partie", "Autre partie du corps", GOALS, Level.DETAIL, Direction.HIGHER_IS_BETTER),

    SHOTS_TAKEN("tirs", "Tirs", SHOTS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    SHOTS_ON_TARGET("tirs_cadres", "Tirs cadrés", SHOTS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    SHOTS_ON_TARGET_RATE("Tirs cadrés (%)", SHOTS, "tirs_cadres", "tirs", 10),
    SHOTS_OFF_TARGET("tirs_non_cadres", "Tirs non cadrés", SHOTS, Level.MAIN, Direction.LOWER_IS_BETTER),
    SHOTS_BLOCKED("tirs_contres", "Tirs contrés", SHOTS, Level.MAIN, Direction.LOWER_IS_BETTER),
    SHOTS_WOODWORK("tirs_poteau", "Poteaux", SHOTS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    SHOTS_SIX_YARD_BOX("tirs_petite_surface", "Dans les 6 mètres", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_PENALTY_AREA("tirs_surface", "Dans la surface", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_OUTSIDE_BOX("tirs_hors_surface", "Hors de la surface", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_OPEN_PLAY("tirs_jeu_ouvert", "En jeu ouvert", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_COUNTER_ATTACK("tirs_contre_attaque", "En contre-attaque", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_SET_PIECE("tirs_coup_arrete", "Sur coup de pied arrêté", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_HEADER("tirs_tete", "De la tête", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_LEFT_FOOT("tirs_pied_gauche", "Du pied gauche", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_RIGHT_FOOT("tirs_pied_droit", "Du pied droit", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SHOTS_OTHER_BODY_PART("tirs_autre_partie", "Autre partie du corps", SHOTS, Level.DETAIL, Direction.HIGHER_IS_BETTER),

    ASSISTS("passes_decisives", "Passes décisives", CREATION, Level.MAIN, Direction.HIGHER_IS_BETTER),
    KEY_PASSES("passes_cles", "Passes clés", CREATION, Level.MAIN, Direction.HIGHER_IS_BETTER),
    ASSISTS_CORNER("pd_corner", "Passes décisives sur corner", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    ASSISTS_CROSS("pd_centre", "Passes décisives sur centre", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    ASSISTS_FREE_KICK("pd_coup_franc", "Passes décisives sur coup franc", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    ASSISTS_THROUGH_BALL("pd_passe_profondeur", "Passes décisives en profondeur", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    ASSISTS_THROW_IN("pd_touche", "Passes décisives sur touche", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    ASSISTS_OTHER("pd_autre", "Autres passes décisives", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_LONG("pc_longues", "Passes clés longues", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_SHORT("pc_courtes", "Passes clés courtes", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_CORNER("pc_corner", "Passes clés sur corner", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_CROSS("pc_centre", "Passes clés sur centre", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_FREE_KICK("pc_coup_franc", "Passes clés sur coup franc", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_THROUGH_BALL("pc_passe_profondeur", "Passes clés en profondeur", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_THROW_IN("pc_touche", "Passes clés sur touche", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    KEY_PASSES_OTHER("pc_autre", "Autres passes clés", CREATION, Level.DETAIL, Direction.HIGHER_IS_BETTER),

    PASSES("passes", "Passes", PASSING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    SUCCESSFUL_PASSES("passes_reussies", "Passes réussies", PASSING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    PASS_ACCURACY("Précision des passes (%)", PASSING, "passes_reussies", "passes", 100),
    SUCCESSFUL_SHORT_PASSES("passes_courtes_reussies", "Passes courtes réussies", PASSING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    MISSED_SHORT_PASSES("passes_courtes_ratees", "Passes courtes ratées", PASSING, Level.DETAIL, Direction.LOWER_IS_BETTER),
    SUCCESSFUL_LONG_PASSES("passes_longues_reussies", "Passes longues réussies", PASSING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    MISSED_LONG_PASSES("passes_longues_ratees", "Passes longues ratées", PASSING, Level.DETAIL, Direction.LOWER_IS_BETTER),

    SUCCESSFUL_CROSSES("centres_reussis", "Centres réussis", CROSSES_AND_SET_PIECES, Level.MAIN, Direction.HIGHER_IS_BETTER),
    MISSED_CROSSES("centres_rates", "Centres ratés", CROSSES_AND_SET_PIECES, Level.MAIN, Direction.LOWER_IS_BETTER),
    SUCCESSFUL_CORNERS("corners_reussis", "Corners réussis", CROSSES_AND_SET_PIECES, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    MISSED_CORNERS("corners_rates", "Corners ratés", CROSSES_AND_SET_PIECES, Level.DETAIL, Direction.LOWER_IS_BETTER),
    SUCCESSFUL_FREE_KICKS("passes_cf_reussies", "Coups francs réussis", CROSSES_AND_SET_PIECES, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    MISSED_FREE_KICKS("passes_cf_ratees", "Coups francs ratés", CROSSES_AND_SET_PIECES, Level.DETAIL, Direction.LOWER_IS_BETTER),

    SUCCESSFUL_DRIBBLES("dribbles_reussis", "Dribbles réussis", DRIBBLING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    DRIBBLE_SUCCESS_RATE("Dribbles réussis (%)", DRIBBLING, "dribbles_reussis", "dribbles_tentes", 10),
    DISPOSSESSIONS("dispossessions", "Dépossessions", DRIBBLING, Level.MAIN, Direction.LOWER_IS_BETTER),
    BALL_LOSSES("pertes_de_balle", "Pertes de balle", DRIBBLING, Level.MAIN, Direction.LOWER_IS_BETTER),
    FOULS_SUFFERED("fautes_subies", "Fautes subies", DRIBBLING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    ATTEMPTED_DRIBBLES("dribbles_tentes", "Dribbles tentés", DRIBBLING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    FAILED_DRIBBLES("dribbles_rates", "Dribbles ratés", DRIBBLING, Level.DETAIL, Direction.LOWER_IS_BETTER),

    TACKLES_WON("tacles_reussis", "Tacles réussis", DEFENDING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    TACKLE_SUCCESS_RATE("Tacles réussis (%)", DEFENDING, "tacles_reussis", "tacles_tentes", 10),
    INTERCEPTIONS("interceptions", "Interceptions", DEFENDING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    CLEARANCES("degagements", "Dégagements", DEFENDING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    BLOCKS("contres", "Contres", DEFENDING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    ATTEMPTED_TACKLES("tacles_tentes", "Tacles tentés", DEFENDING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    BLOCKED_PASSES("passes_contrees", "Passes contrées", DEFENDING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    BLOCKED_CROSSES("centres_contres", "Centres contrés", DEFENDING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    DRIBBLED_PAST("dribbles_subis", "Dribbles subis", DEFENDING, Level.DETAIL, Direction.LOWER_IS_BETTER),

    AERIAL_DUELS_WON("duels_aeriens_gagnes", "Duels aériens gagnés", AERIAL_DUELS, Level.MAIN, Direction.HIGHER_IS_BETTER),
    AERIAL_DUEL_WIN_RATE("Duels aériens gagnés (%)", AERIAL_DUELS, "duels_aeriens_gagnes", "duels_aeriens", 10),
    AERIAL_DUELS_TOTAL("duels_aeriens", "Duels aériens disputés", AERIAL_DUELS, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    AERIAL_DUELS_LOST("duels_aeriens_perdus", "Duels aériens perdus", AERIAL_DUELS, Level.DETAIL, Direction.LOWER_IS_BETTER),

    FOULS_COMMITTED("fautes_commises", "Fautes commises", DISCIPLINE, Level.MAIN, Direction.LOWER_IS_BETTER),
    YELLOW_CARDS("cartons_jaunes", "Cartons jaunes", DISCIPLINE, Level.MAIN, Direction.LOWER_IS_BETTER),
    RED_CARDS("cartons_rouges", "Cartons rouges", DISCIPLINE, Level.MAIN, Direction.LOWER_IS_BETTER),
    OFFSIDES("hors_jeu", "Hors-jeu", DISCIPLINE, Level.MAIN, Direction.LOWER_IS_BETTER),

    SAVES("arrets", "Arrêts", GOALKEEPING, Level.MAIN, Direction.HIGHER_IS_BETTER),
    SAVES_SIX_YARD_BOX("arrets_petite_surface", "Arrêts dans les 6 mètres", GOALKEEPING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SAVES_PENALTY_AREA("arrets_surface", "Arrêts dans la surface", GOALKEEPING, Level.DETAIL, Direction.HIGHER_IS_BETTER),
    SAVES_OUTSIDE_BOX("arrets_hors_surface", "Arrêts hors de la surface", GOALKEEPING, Level.DETAIL, Direction.HIGHER_IS_BETTER),

    MATCHES_PLAYED("matchs_joues", "Matchs joués", PLAYING_TIME, Level.MAIN, Direction.HIGHER_IS_BETTER, Scaling.TOTAL_ONLY),
    STARTS("titularisations", "Titularisations", PLAYING_TIME, Level.MAIN, Direction.HIGHER_IS_BETTER, Scaling.TOTAL_ONLY),
    MINUTES_PLAYED("minutes", "Minutes", PLAYING_TIME, Level.MAIN, Direction.HIGHER_IS_BETTER, Scaling.TOTAL_ONLY),
    PLAYER_OF_THE_MATCH("homme_du_match", "Homme du match", PLAYING_TIME, Level.MAIN, Direction.HIGHER_IS_BETTER, Scaling.TOTAL_ONLY),
    SUBSTITUTE_APPEARANCES("entrees_en_jeu", "Entrées en jeu", PLAYING_TIME, Level.DETAIL, Direction.HIGHER_IS_BETTER, Scaling.TOTAL_ONLY);

    public enum Level { MAIN, DETAIL }

    public enum Direction { HIGHER_IS_BETTER, LOWER_IS_BETTER }

    public enum Scaling { PER_90, TOTAL_ONLY }

    private final String column;
    private final String label;
    private final StatCategory category;
    private final Level level;
    private final Direction direction;
    private final Scaling scaling;
    private final String numeratorColumn;
    private final String denominatorColumn;
    private final int minimumAttempts;

    Stat(String column, String label, StatCategory category, Level level, Direction direction) {
        this(column, label, category, level, direction, Scaling.PER_90);
    }

    Stat(String column, String label, StatCategory category, Level level, Direction direction, Scaling scaling) {
        this.column = column;
        this.label = label;
        this.category = category;
        this.level = level;
        this.direction = direction;
        this.scaling = scaling;
        this.numeratorColumn = null;
        this.denominatorColumn = null;
        this.minimumAttempts = 0;
    }

    Stat(String label, StatCategory category, String numeratorColumn, String denominatorColumn, int minimumAttempts) {
        this.column = null;
        this.label = label;
        this.category = category;
        this.level = Level.MAIN;
        this.direction = Direction.HIGHER_IS_BETTER;
        this.scaling = Scaling.TOTAL_ONLY;
        this.numeratorColumn = numeratorColumn;
        this.denominatorColumn = denominatorColumn;
        this.minimumAttempts = minimumAttempts;
    }

    public static List<Stat> byCategory(StatCategory category) {
        return Arrays.stream(values())
                .filter(stat -> stat.category == category)
                .toList();
    }

    public static List<String> columns() {
        return Arrays.stream(values())
                .map(stat -> stat.column)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean isRatio() {
        return column == null;
    }

    public boolean isDetail() {
        return level == Level.DETAIL;
    }

    public boolean isLowerBetter() {
        return direction == Direction.LOWER_IS_BETTER;
    }

    public boolean isPer90Applicable() {
        return scaling == Scaling.PER_90;
    }

    public String getColumn() {
        return column;
    }

    public String getLabel() {
        return label;
    }

    public StatCategory getCategory() {
        return category;
    }

    public String getNumeratorColumn() {
        return numeratorColumn;
    }

    public String getDenominatorColumn() {
        return denominatorColumn;
    }

    public int getMinimumAttempts() {
        return minimumAttempts;
    }
}
