#!/usr/bin/env python3
"""Calcule les notes joueurs (70-99) et genere initdb/03_notes.sql.

Usage :
    docker exec -i api_database sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/export_stats.sql > stats.csv
    python3 scripts/ratings.py stats.csv > initdb/03_notes.sql
    python3 scripts/ratings.py stats.csv --top 100

Principe :
    1. performance : taux par 90 min, standardises par (saison, ligne), ponderes selon le poste ;
       la note moyenne WhoScored est un des criteres. Le resultat est ramene vers 0 quand le
       joueur a peu de minutes (fiabilite).
    2. volume : un joueur qui joue beaucoup dans la competition vaut plus.
    3. parcours : bonus croissant selon le tour atteint par l'equipe (vainqueur > finaliste > ... ),
       module par la qualite du joueur : un joueur sous la moyenne en profite beaucoup moins.
       A performance egale, un vainqueur est mieux note qu'un elimine en huitiemes.
    4. echelle : transformation monotone vers 70-99, le pire joueur de la base a 70, le meilleur 99.
"""
import argparse
import csv
import math
import statistics
import sys
from collections import defaultdict

MIN_RATING = 70
MAX_RATING = 99
REFERENCE_MINUTES = 270
RELIABILITY_MINUTES = 450
STAGE_FULL_EFFECT_MINUTES = 900
VOLUME_WEIGHT = 0.30
STAGE_BONUS_WEIGHT = 1.0
STAGE_QUALITY_SLOPE = 0.8
STAGE_GAIN_MIN = 0.2
STAGE_GAIN_MAX = 1.5
RANK_SHARE = 0.3
RANK_EXPONENT = 4.0
GAP_EXPONENT = 1.4

STAGE_BONUS = {
    "Phase de ligue": 0.0,
    "Phase de groupes": 0.0,
    "Barrages": 0.10,
    "Huitièmes de finale": 0.25,
    "Quarts de finale": 0.50,
    "Demi-finales": 0.75,
    "Finale": 1.00,
    "Vainqueur": 1.45,
}

GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD = "Goalkeeper", "Defender", "Midfielder", "Forward"

CRITERIA = {
    FORWARD: {"goals": 0.22, "assists": 0.12, "keyPasses": 0.08, "shotsOnTarget": 0.06, "dribbles": 0.06,
              "motm": 0.08, "turnovers": -0.03, "avgRating": 0.60},
    MIDFIELDER: {"goals": 0.10, "assists": 0.12, "keyPasses": 0.12, "defensiveActions": 0.10, "passes": 0.08,
                 "passAccuracy": 0.05, "dribbles": 0.04, "motm": 0.07, "avgRating": 0.60},
    DEFENDER: {"defensiveActions": 0.25, "aerials": 0.08, "passes": 0.06, "passAccuracy": 0.04,
               "goalContribution": 0.08, "motm": 0.06, "fouls": -0.03, "avgRating": 0.80},
    GOALKEEPER: {"saves": 0.20, "passes": 0.05, "passAccuracy": 0.05, "motm": 0.10, "avgRating": 0.80},
}

LINES_BY_POSITION_CODE = {
    "GK": [GOALKEEPER], "Goalkeeper": [GOALKEEPER],
    "D": [DEFENDER], "Defender": [DEFENDER],
    "DMC": [MIDFIELDER], "M": [MIDFIELDER], "Midfielder": [MIDFIELDER],
    "AM": [MIDFIELDER],
    "FW": [FORWARD], "Forward": [FORWARD],
}


def rated_line(occupied_positions, fallback):
    codes = [token.split("(")[0].strip() for token in occupied_positions.split(",")]
    lines = [line for code in codes for line in LINES_BY_POSITION_CODE.get(code, [])]
    if FORWARD in lines:
        return FORWARD
    return lines[0] if lines else fallback


def per90(row, column):
    return int(row[column]) * 90.0 / max(int(row["minutes"]), 1)


def features(row):
    passes = int(row["passes"])
    return {
        "goals": per90(row, "buts"),
        "assists": per90(row, "ast"),
        "keyPasses": per90(row, "kp"),
        "shotsOnTarget": per90(row, "sot"),
        "dribbles": per90(row, "drb"),
        "motm": per90(row, "motm"),
        "turnovers": per90(row, "turn"),
        "defensiveActions": (int(row["tkl"]) + int(row["intc"]) + int(row["clr"]) + int(row["blk"]))
                            * 90.0 / max(int(row["minutes"]), 1),
        "aerials": per90(row, "aer"),
        "passes": per90(row, "passes"),
        "passAccuracy": int(row["passes_ok"]) / passes if passes >= 30 else None,
        "goalContribution": (int(row["buts"]) + int(row["ast"])) * 90.0 / max(int(row["minutes"]), 1),
        "fouls": per90(row, "fouls"),
        "saves": per90(row, "saves"),
        "avgRating": float(row["note_moyenne"]),
    }


def build_references(rows):
    samples = defaultdict(lambda: defaultdict(list))
    for row in rows:
        if int(row["minutes"]) < REFERENCE_MINUTES:
            continue
        values = features(row)
        for name, value in values.items():
            if value is not None:
                samples[(row["annee_debut"], row["ratedLine"])][name].append(value)
    return {
        key: {name: (statistics.mean(values), statistics.pstdev(values) or 1.0) for name, values in by_name.items()}
        for key, by_name in samples.items()
    }


def line_quality(row, line, references):
    values = features(row)
    reference = references[(row["annee_debut"], line)]
    weights = CRITERIA[line]
    total_weight = sum(abs(weight) for name, weight in weights.items() if values[name] is not None)
    score = sum(
        weight * (values[name] - reference[name][0]) / reference[name][1]
        for name, weight in weights.items() if values[name] is not None)
    return score / total_weight if total_weight else 0.0


def reliability(row):
    minutes = int(row["minutes"])
    return minutes / (minutes + RELIABILITY_MINUTES)


def volume(row):
    return math.log(max(int(row["minutes"]), 1) / 450.0)


def stage_label(row):
    return "Vainqueur" if row["vainqueur"] == "t" else row["tour_elimination"]


def stage_effect(row):
    return STAGE_BONUS[stage_label(row)] * min(1.0, int(row["minutes"]) / STAGE_FULL_EFFECT_MINUTES)


def latent_score(row, references):
    quality = line_quality(row, row["ratedLine"], references)
    stage_gain = min(STAGE_GAIN_MAX, max(STAGE_GAIN_MIN, 1 + STAGE_QUALITY_SLOPE * quality))
    return (quality * reliability(row) * 3.0
            + VOLUME_WEIGHT * volume(row) * reliability(row)
            + STAGE_BONUS_WEIGHT * stage_effect(row) * stage_gain)


def to_rating(latent, lowest, highest, percentile):
    gap = (latent - lowest) / (highest - lowest)
    position = RANK_SHARE * percentile ** RANK_EXPONENT + (1 - RANK_SHARE) * gap ** GAP_EXPONENT
    return round(MIN_RATING + (MAX_RATING - MIN_RATING) * position)


def compute_ratings(rows):
    for row in rows:
        row["ratedLine"] = rated_line(row["postes_occupes"], row["poste_note"])
    references = build_references(rows)
    latents = [latent_score(row, references) for row in rows]
    lowest, highest = min(latents), max(latents)
    ranks = {index: rank for rank, index in enumerate(sorted(range(len(latents)), key=latents.__getitem__))}
    for index, (row, latent) in enumerate(zip(rows, latents)):
        row["rating"] = to_rating(latent, lowest, highest, ranks[index] / (len(rows) - 1))
    return rows


def render_sql(rows):
    values = ",\n".join(
        "({}, {}, {}, {}, '{}')".format(row["annee_debut"], row["joueur_id"], row["equipe_id"],
                                       row["rating"], row["ratedLine"])
        for row in rows)
    return f"""-- Notes joueurs ({MIN_RATING}-{MAX_RATING}), calculees par scripts/ratings.py. A executer apres 02_data.sql.
BEGIN;
ALTER TABLE stats_joueur_saison ADD COLUMN note smallint CHECK (note BETWEEN {MIN_RATING} AND {MAX_RATING});
ALTER TABLE stats_joueur_saison ADD COLUMN poste_note text CHECK (poste_note IN ('Goalkeeper','Defender','Midfielder','Forward'));
COMMENT ON COLUMN stats_joueur_saison.note IS 'Note {MIN_RATING}-{MAX_RATING} par joueur et saison : performance par poste (poste_note), volume de jeu et parcours de l''equipe en Ligue des champions (voir scripts/ratings.py). Independante de note_moyenne (WhoScored).';
UPDATE stats_joueur_saison s SET note = v.note, poste_note = v.poste_note
FROM (VALUES
{values}
) AS v(annee_debut, joueur_id, equipe_id, note, poste_note)
WHERE s.annee_debut = v.annee_debut AND s.joueur_id = v.joueur_id AND s.equipe_id = v.equipe_id;
ALTER TABLE stats_joueur_saison ALTER COLUMN note SET NOT NULL;
COMMIT;
"""


def render_top(rows, size):
    ranked = sorted(rows, key=lambda row: (-row["rating"], -int(row["minutes"])))[:size]
    header = "{:>3}  {:<26} {:<18} {:<8} {:<10} {:>4}  {}".format("#", "Joueur", "Equipe", "Saison", "Poste", "Note", "Parcours")
    lines = ["{:>3}  {:<26} {:<18} {:<8} {:<10} {:>4}  {}".format(
        index, row["nom"][:26], row["equipe"][:18], f"{row['annee_debut']}/{int(row['annee_debut']) + 1 - 2000:02d}",
        row["ratedLine"], row["rating"], stage_label(row)) for index, row in enumerate(ranked, 1)]
    return "\n".join([header] + lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("csv_path")
    parser.add_argument("--top", type=int)
    parser.add_argument("--summary", action="store_true")
    arguments = parser.parse_args()
    with open(arguments.csv_path, newline="", encoding="utf-8") as handle:
        rows = compute_ratings(list(csv.DictReader(handle)))
    if arguments.top:
        print(render_top(rows, arguments.top))
    elif arguments.summary:
        print_summary(rows)
    else:
        sys.stdout.write(render_sql(rows))


def print_summary(rows):
    ratings = sorted(row["rating"] for row in rows)
    regulars = sorted(row["rating"] for row in rows if int(row["minutes"]) >= REFERENCE_MINUTES)
    quantile = lambda data, share: data[int(share * (len(data) - 1))]
    print("all", len(ratings), ratings[0], ratings[-1], [quantile(ratings, s) for s in (.1, .25, .5, .75, .9, .99)])
    print("regulars", len(regulars), [quantile(regulars, s) for s in (.1, .25, .5, .75, .9, .99)])
    by_stage = defaultdict(list)
    for row in rows:
        if int(row["minutes"]) >= 450:
            by_stage[stage_label(row)].append(row["rating"])
    for stage, values in sorted(by_stage.items(), key=lambda item: statistics.mean(item[1])):
        print(f"{stage:<22}{len(values):>5}  {statistics.mean(values):.1f}")


if __name__ == "__main__":
    main()
