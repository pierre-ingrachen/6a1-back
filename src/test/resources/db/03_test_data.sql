INSERT INTO equipe (equipe_id, nom, pays) VALUES
(1, 'Alpha FC', 'England'),
(2, 'Beta United', 'Spain'),
(3, 'Gamma Club', 'Germany');

INSERT INTO joueur (joueur_id, nom, poste, taille_cm, poids_kg, age_a_la_collecte) VALUES
(11119, 'Lionel Messi', 'Forward', 170, 72, 39),
(2, 'Kylian Mbappé', 'Forward', 178, 73, 27),
(3, 'Gianluigi Keeper', 'Goalkeeper', NULL, NULL, 30),
(4, 'Multi Club', 'Midfielder', 180, 75, 28);

INSERT INTO saison (annee_debut, libelle, whoscored_season_id, whoscored_stage_id, nb_matchs_officiel, nb_buts_officiel) VALUES
(2021, '2021/2022', 1, 1, 125, 380),
(2022, '2022/2023', 2, 2, 125, 360);

INSERT INTO parcours_equipe_saison (annee_debut, equipe_id, tour_elimination, vainqueur) VALUES
(2021, 1, 'Quarts de finale', false),
(2021, 2, 'Phase de groupes', false),
(2021, 3, NULL, true),
(2022, 1, 'Barrages', false);

INSERT INTO stats_joueur_saison
(annee_debut, joueur_id, equipe_id, matchs_joues, entrees_en_jeu, minutes, buts, tirs, tirs_cadres,
 passes, passes_courtes_reussies, passes_longues_reussies, arrets, passes_decisives, note, poste_note, postes_occupes, note_moyenne) VALUES
(2021, 11119, 1, 8, 2, 640, 5, 20, 10, 150, 100, 20, 0, 4, 86, 'Forward', 'FW', 7.85),
(2021, 2, 3, 10, 0, 900, 7, 30, 15, 120, 70, 10, 0, 0, 90, 'Forward', 'FW', 7.40),
(2021, 3, 2, 6, 0, 540, 0, 0, 0, 200, 120, 30, 20, 0, 78, 'Goalkeeper', 'GK', 6.90),
(2021, 4, 1, 3, 1, 120, 0, 2, 1, 40, 20, 5, 0, 0, 74, 'Midfielder', 'M(C)', 6.50),
(2021, 4, 2, 5, 1, 300, 1, 4, 2, 80, 50, 10, 0, 0, 75, 'Midfielder', 'M(C)', 6.80),
(2022, 11119, 1, 4, 0, 360, 2, 10, 5, 60, 40, 5, 0, 0, 84, 'Forward', 'FW', 7.10);
