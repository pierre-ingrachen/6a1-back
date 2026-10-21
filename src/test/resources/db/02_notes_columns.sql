ALTER TABLE stats_joueur_saison ADD COLUMN note smallint CHECK (note BETWEEN 70 AND 99);
ALTER TABLE stats_joueur_saison ADD COLUMN poste_note text CHECK (poste_note IN ('Goalkeeper','Defender','Midfielder','Forward'));
