-- Champions League 2010/11 -> 2025/26 : statistiques joueurs par saison (source : WhoScored).
-- PostgreSQL 14+. A executer avant 02_data.sql.
BEGIN;

DROP VIEW IF EXISTS vue_stats_joueur_saison;
DROP TABLE IF EXISTS parcours_equipe_saison;
DROP TABLE IF EXISTS stats_joueur_saison;
DROP TABLE IF EXISTS saison;
DROP TABLE IF EXISTS joueur;
DROP TABLE IF EXISTS equipe;

CREATE TABLE equipe (
    equipe_id integer PRIMARY KEY,            -- id WhoScored
    nom       text NOT NULL,                  -- nom abrege du site (ex. 'Man City')
    pays      text NOT NULL
);

CREATE TABLE joueur (
    joueur_id         integer PRIMARY KEY,    -- id WhoScored
    nom               text NOT NULL,
    poste             text NOT NULL CHECK (poste IN ('Goalkeeper','Defender','Midfielder','Forward')),
    taille_cm         smallint CHECK (taille_cm > 0),   -- NULL = inconnu
    poids_kg          smallint CHECK (poids_kg > 0),    -- NULL = inconnu
    age_a_la_collecte smallint NOT NULL       -- age en oct. 2026, PAS l'age pendant la saison
);

CREATE TABLE saison (
    annee_debut          smallint PRIMARY KEY,  -- 2010 pour 2010/2011
    libelle              text NOT NULL UNIQUE,
    whoscored_season_id  integer NOT NULL,
    whoscored_stage_id   integer NOT NULL,      -- stage agregeant toute la saison
    nb_matchs_officiel   smallint NOT NULL,     -- phase finale, hors qualifications (Wikipedia)
    nb_buts_officiel     smallint NOT NULL      -- inclut les buts contre son camp
);

CREATE TABLE stats_joueur_saison (
    annee_debut smallint NOT NULL REFERENCES saison(annee_debut),
    joueur_id   integer  NOT NULL REFERENCES joueur(joueur_id),
    equipe_id   integer  NOT NULL REFERENCES equipe(equipe_id),
    matchs_joues smallint NOT NULL,
    entrees_en_jeu smallint,
    minutes smallint NOT NULL,
    homme_du_match smallint,
    buts smallint,
    buts_contre_son_camp smallint,
    passes_decisives smallint,
    penaltys_marques smallint,
    penaltys_tires smallint,
    buts_petite_surface smallint,
    buts_surface smallint,
    buts_hors_surface smallint,
    buts_jeu_ouvert smallint,
    buts_contre_attaque smallint,
    buts_coup_arrete smallint,
    buts_normal smallint,
    buts_tete smallint,
    buts_pied_gauche smallint,
    buts_pied_droit smallint,
    buts_autre_partie smallint,
    pd_corner smallint,
    pd_centre smallint,
    pd_coup_franc smallint,
    pd_autre smallint,
    pd_passe_profondeur smallint,
    pd_touche smallint,
    tirs smallint,
    tirs_cadres smallint,
    tirs_non_cadres smallint,
    tirs_contres smallint,
    tirs_poteau smallint,
    tirs_petite_surface smallint,
    tirs_surface smallint,
    tirs_hors_surface smallint,
    tirs_jeu_ouvert smallint,
    tirs_contre_attaque smallint,
    tirs_coup_arrete smallint,
    tirs_tete smallint,
    tirs_pied_gauche smallint,
    tirs_pied_droit smallint,
    tirs_autre_partie smallint,
    tacles_reussis smallint,
    tacles_tentes smallint,
    dribbles_subis smallint,
    interceptions smallint,
    degagements smallint,
    contres smallint,
    passes_contrees smallint,
    centres_contres smallint,
    fautes_commises smallint,
    fautes_subies smallint,
    hors_jeu smallint,
    cartons_jaunes smallint,
    cartons_rouges smallint,
    passes smallint,
    passes_courtes_reussies smallint,
    passes_courtes_ratees smallint,
    passes_longues_reussies smallint,
    passes_longues_ratees smallint,
    centres_reussis smallint,
    centres_rates smallint,
    corners_reussis smallint,
    corners_rates smallint,
    passes_cf_reussies smallint,
    passes_cf_ratees smallint,
    passes_cles smallint,
    pc_longues smallint,
    pc_courtes smallint,
    pc_corner smallint,
    pc_centre smallint,
    pc_coup_franc smallint,
    pc_autre smallint,
    pc_passe_profondeur smallint,
    pc_touche smallint,
    dribbles_tentes smallint,
    dribbles_reussis smallint,
    dribbles_rates smallint,
    dispossessions smallint,
    pertes_de_balle smallint,
    duels_aeriens smallint,
    duels_aeriens_gagnes smallint,
    duels_aeriens_perdus smallint,
    arrets smallint,
    arrets_petite_surface smallint,
    arrets_surface smallint,
    arrets_hors_surface smallint,
    note_moyenne numeric(4,2),
    postes_occupes text,
    PRIMARY KEY (annee_debut, joueur_id, equipe_id),
    CHECK (matchs_joues > 0),
    CHECK (entrees_en_jeu <= matchs_joues),
    CHECK (minutes >= 0),
    CHECK (tirs_cadres <= tirs),
    CHECK (tacles_reussis <= tacles_tentes),
    CHECK (duels_aeriens = duels_aeriens_gagnes + duels_aeriens_perdus),
    CHECK (dribbles_tentes = dribbles_reussis + dribbles_rates),
    CHECK (note_moyenne BETWEEN 0 AND 10)
);

CREATE TABLE parcours_equipe_saison (
    annee_debut     smallint NOT NULL REFERENCES saison(annee_debut),
    equipe_id       integer  NOT NULL REFERENCES equipe(equipe_id),
    tour_elimination text CHECK (tour_elimination IN ('Phase de groupes','Phase de ligue','Barrages',
                                 'Huitièmes de finale','Quarts de finale','Demi-finales','Finale')),
    vainqueur       boolean  NOT NULL,
    PRIMARY KEY (annee_debut, equipe_id),
    CHECK (vainqueur = (tour_elimination IS NULL))   -- NULL uniquement pour le vainqueur
);
COMMENT ON TABLE parcours_equipe_saison IS 'Une ligne par equipe engagee en phase finale (hors qualifications). tour_elimination = dernier tour joue ; Finale = finaliste battu ; NULL = vainqueur. Phase de ligue a partir de 2024/25, Barrages = barrages de la phase de ligue.';

CREATE INDEX idx_stats_classement_buts ON stats_joueur_saison (annee_debut, buts DESC);
CREATE INDEX idx_stats_joueur          ON stats_joueur_saison (joueur_id);
CREATE INDEX idx_stats_equipe_saison   ON stats_joueur_saison (equipe_id, annee_debut);

COMMENT ON TABLE  stats_joueur_saison IS 'Totaux sur la competition. Un joueur peut avoir 2 lignes la meme saison (2 clubs, a partir de 2019/20).';
COMMENT ON COLUMN stats_joueur_saison.matchs_joues IS 'Apparitions totales (titulaire ou remplacant).';
COMMENT ON COLUMN stats_joueur_saison.entrees_en_jeu IS 'Dont matchs commences sur le banc.';
COMMENT ON COLUMN stats_joueur_saison.buts IS 'Hors buts contre son camp (voir buts_contre_son_camp).';
COMMENT ON COLUMN stats_joueur_saison.tirs IS 'Total de tirs. Les sous-categories (cadres, non cadres, contres, poteau) ne sont pas additives (8 % des lignes).';
COMMENT ON COLUMN stats_joueur_saison.buts_petite_surface IS 'Les colonnes buts_* de detail peuvent etre gonflees de 1 pour les buts contre son camp en 2010/11-2013/14.';
COMMENT ON COLUMN stats_joueur_saison.interceptions IS 'Serie probablement non comparable entre saisons eloignees (rupture de definition suspectee).';
COMMENT ON COLUMN stats_joueur_saison.postes_occupes IS 'Postes occupes dans la saison (ex. M(CLR),FW).';

-- Vue de confort pour le back-end : noms + colonnes derivees.
CREATE VIEW vue_stats_joueur_saison AS
SELECT s.*,
       j.nom AS joueur_nom, j.poste, e.nom AS equipe_nom, e.pays AS equipe_pays, sa.libelle AS saison,
       s.matchs_joues - s.entrees_en_jeu                                  AS titularisations,
       s.passes_courtes_reussies + s.passes_longues_reussies               AS passes_reussies,
       round(100.0 * (s.passes_courtes_reussies + s.passes_longues_reussies) / NULLIF(s.passes, 0), 1) AS precision_passes_pct,
       round(s.minutes * 1.0 / NULLIF(s.matchs_joues, 0), 1)               AS minutes_par_match
FROM stats_joueur_saison s
JOIN joueur j USING (joueur_id)
JOIN equipe e USING (equipe_id)
JOIN saison sa USING (annee_debut);

COMMIT;
