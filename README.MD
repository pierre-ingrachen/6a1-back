# 6a1-back — API Champions League Dream Team

API REST Spring Boot qui sert deux usages :

- **Catalogue** : consultation des équipes, saisons et statistiques de joueurs de la Ligue des champions, de 2010/11 à 2025/26 (source : WhoScored).
- **Jeu « Dream Team »** : on compose une équipe de 11 joueurs en piochant des effectifs de clubs tirés au hasard, puis le back simule une campagne de Ligue des champions complète avec cette équipe.

Le front associé est le dépôt [6a1-front](https://github.com/pierre-ingrachen/6a1-front).

## Sommaire

1. [Stack](#stack)
2. [Démarrage rapide](#démarrage-rapide)
3. [API](#api)
4. [Règles du jeu](#règles-du-jeu)
5. [Architecture](#architecture)
6. [Base de données](#base-de-données)
7. [Configuration d'exécution](#configuration-dexécution)
8. [Tests et qualité](#tests-et-qualité)
9. [Intégration continue et hooks git](#intégration-continue-et-hooks-git)

## Stack

| Élément | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.1.3 (Web, Data JPA, Validation) |
| Base de données | PostgreSQL 16 (Docker) |
| Documentation d'API | springdoc-openapi 2.2.0 (Swagger UI) |
| Tests | JUnit 5, Spring Boot Test, Testcontainers 1.21.4 |
| Couverture | JaCoCo 0.8.12 |

## Démarrage rapide

Prérequis : Java 17, Docker (lancé).

1. Copier `.env.sample` en `.env` :
   ```bash
   cp .env.sample .env
   ```
2. Renseigner dans `.env` les identifiants de ton choix :
   ```
   DATABASE_USER=
   DATABASE_PASSWORD=
   DATABASE_NAME=
   ```
   Le fichier `.env` est ignoré par git : ne jamais le committer.
3. Démarrer la base :
   ```bash
   docker compose up -d
   ```
   Au premier démarrage, PostgreSQL charge automatiquement les scripts de `initdb/` (voir [Base de données](#base-de-données)). Le chargement dure quelques dizaines de secondes ; le `healthcheck` du conteneur indique quand la base est prête.
4. Lancer l'API avec les variables du `.env` :
   - **IntelliJ** : installer le plugin [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile), puis ajouter le `.env` à la configuration d'exécution (penser à afficher les fichiers cachés) :
     <img src="img-readme/img.png">
     <img src="img-readme/img_1.png">
     <img src="img-readme/img_2.png">
   - **Terminal** :
     ```bash
     set -a; source .env; set +a
     SWAGGER_ENABLED=true ./mvnw spring-boot:run
     ```
   - **Depuis le front** : `npm run back` dans `6a1-front` (charge le `.env` automatiquement, voir le README du front).

L'API écoute sur `http://localhost:8080`. Avec `SWAGGER_ENABLED=true`, la documentation interactive est sur `http://localhost:8080/swagger-ui/index.html` (spécification : `/v3/api-docs`).

Pour arrêter la base : `docker compose down` (ajouter `-v` pour supprimer aussi les données et forcer un rechargement depuis `initdb/`).

## API

Toutes les erreurs ont le même corps : `{ "message": "..." }`.

### Catalogue (lecture seule)

| Méthode | Chemin | Description | Erreurs |
|---|---|---|---|
| `GET` | `/seasons` | Toutes les saisons | |
| `GET` | `/teams` | Toutes les équipes | |
| `GET` | `/teams/{teamId}` | Une équipe | `404` équipe inconnue |
| `GET` | `/teams/{teamId}/seasons` | Saisons jouées par l'équipe | `404` |
| `GET` | `/teams/{teamId}/players?season=` | Effectif de l'équipe pour une saison | `404`, `400` saison invalide |
| `GET` | `/players/search?query=&season=` | Recherche de joueurs par nom (`query` ≤ 100 caractères, `season` optionnelle) | `400` |
| `GET` | `/players/{playerId}/seasons` | Saisons et équipes d'un joueur | `404` |
| `GET` | `/players/{playerId}/stats?season=` | Statistiques par catégorie, avec percentiles de la saison | `404`, `400` |

`/seasons`, `/teams` et `/formations` sont mis en cache (voir [Configuration d'exécution](#configuration-dexécution)).

### Jeu

| Méthode | Chemin | Corps | Réponse | Description |
|---|---|---|---|---|
| `GET` | `/formations` | | `200` | Les 21 formations disponibles |
| `POST` | `/games` | `{ "formation": "4-3-3" }` | `201` | Crée une partie et tire la première équipe |
| `GET` | `/games/{gameId}` | | `200` | État de la partie |
| `POST` | `/games/{gameId}/picks` | `{ "playerId", "slotIndex" }` | `200` | Place un joueur de l'équipe tirée sur un poste libre |
| `POST` | `/games/{gameId}/moves` | `{ "fromSlot", "toSlot" }` | `200` | Déplace ou échange deux joueurs déjà placés |
| `POST` | `/games/{gameId}/redraws` | `{ "type": "SEASON" \| "TEAM" }` | `200` | Utilise un re-tirage |
| `POST` | `/games/{gameId}/simulation` | | `200` | Simule la compétition (draft complète requise) et supprime la partie |

Codes d'erreur du jeu :

| Code | Cas |
|---|---|
| `400` | Corps invalide, formation inconnue, choix de joueur ou de poste invalide |
| `404` | Partie inconnue ou expirée |
| `409` | Draft déjà terminée, draft incomplète pour la simulation, aucun re-tirage possible |
| `429` | Trop de créations de parties depuis la même IP, ou trop de clients actifs suivis par le limiteur (plus de 10 000 dans la fenêtre) |
| `503` | Nombre maximal de parties actives atteint |

## Règles du jeu

1. **Formation** : on choisit parmi 21 formations (4-3-3, 3-5-2, 5-4-1…). Chacune définit 11 postes.
2. **Draft** : à chaque tirage, une équipe-saison (ex. « Real Madrid 2017/18 ») est proposée. On y choisit un joueur ayant joué au moins 270 minutes cette saison-là, et un poste libre où il peut jouer. Un joueur déjà pris n'est plus proposé.
3. **Re-tirages** : 3 par partie. `SEASON` garde le club et change de saison ; `TEAM` garde la saison et change de club. Une équipe-saison déjà tirée ne revient pas.
4. **Notes** : chaque joueur a une note de 70 à 99 par saison (voir [Base de données](#base-de-données)). Les notes de l'équipe, pondérées par ligne (le gardien compte plus), donnent une force d'attaque et de défense.
5. **Simulation** : la force de l'équipe est légèrement pénalisée (handicap). Elle joue une phase de groupes (3 adversaires, matchs aller-retour) puis des matchs à élimination directe (elle est toujours qualifiée : son rang en groupe fixe le niveau de l'adversaire en huitièmes) : huitièmes, quarts, demies, finale (avec prolongations et tirs au but si besoin). Les adversaires sont des équipes-saisons réelles du catalogue ; leur niveau dépend du classement en groupe.
6. **Reproductibilité** : chaque partie a une graine ; tirages et simulation sont déterministes pour une même graine.

Les parties sont **en mémoire** (aucune table) et purgées automatiquement : après 15 minutes d'inactivité si aucun joueur n'a été choisi, après 2 heures sinon, et dès la simulation.

## Architecture

Architecture en couches, sans logique métier dans les contrôleurs. Le code est en anglais ; les noms de tables et colonnes SQL sont en français (hérités du jeu de données).

```
src/main/java/com/takima/backskeleton/
├── controllers/   Catalogue : SeasonController, TeamController, PlayerController
├── services/      Logique du catalogue, index de recherche, calcul des percentiles, cache des saisons
├── DAO/           Repositories Spring Data JPA et projections
├── models/        Entités JPA
├── DTO/           Records exposés par l'API
├── exceptions/    Exceptions métier et gestionnaires d'erreurs du catalogue
├── config/        CORS, cache HTTP du catalogue, horloge
└── game/          Fonctionnalité jeu, organisée dans les mêmes couches
    ├── controller/   GameController, FormationController, GameExceptionHandler
    ├── service/      Draft, tirage, force d'équipe, simulation de matchs et de compétition,
    │                 verrous par partie, limiteur de débit, nettoyage planifié
    ├── dao/          Stockage des parties en mémoire, lecture JDBC des effectifs
    ├── model/        Game, Formation, Round, TeamSeason…
    ├── dto/          Requêtes et vues exposées
    └── exception/    Exceptions du jeu
```

Points notables :

- **Records** pour les DTO et les modèles du jeu.
- **Concurrence** : un verrou par partie (`GameLocks`) sérialise les actions sur une même partie ; la création est protégée par un verrou global qui applique la limite de parties actives.
- **Catalogue chargé à la demande** : les statistiques d'une saison sont mises en cache avec expiration pour calculer les percentiles sans requête répétée.

## Base de données

PostgreSQL 16, scripts exécutés par ordre alphabétique au premier démarrage du conteneur :

| Fichier | Contenu |
|---|---|
| `initdb/01_schema.sql` | Tables `equipe`, `joueur`, `saison`, `stats_joueur_saison` et `parcours_equipe_saison` (tour atteint par chaque équipe) |
| `initdb/02_data.sql` | Données de la Ligue des champions 2010/11 → 2025/26 |
| `initdb/03_notes.sql` | Colonnes `note` et `poste_note` de `stats_joueur_saison` (note 70-99 par joueur et saison) |

`03_notes.sql` est généré par `scripts/ratings.py` à partir des statistiques : performance par poste, volume de jeu et parcours de l'équipe en compétition. Pour le régénérer (base démarrée, depuis la racine du dépôt) :

```bash
docker exec -i api_database sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/export_stats.sql > stats.csv
python3 scripts/ratings.py stats.csv > initdb/03_notes.sql
```

L'export ne dépend que des tables de `01_schema.sql`. Pour appliquer le nouveau fichier, recréer la base (`docker compose down -v` puis `docker compose up -d`).

Si un ancien conteneur PostgreSQL d'une autre version existe, lancer `docker compose down -v` puis `docker compose up -d` : la base est rechargée depuis `initdb/`.

## Configuration d'exécution

| Variable / propriété | Défaut | Rôle |
|---|---|---|
| `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_NAME` | — (obligatoires) | Connexion PostgreSQL (`localhost:5432`) |
| `SWAGGER_ENABLED` | `false` | Active `/swagger-ui/index.html` et `/v3/api-docs` (`npm run back` côté front l'active en local) |
| `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` en prod) | `http://localhost:4200` | Origines autorisées, séparées par des virgules |
| `game.creation.rate-limit.max-requests` | `20` | Créations de parties par fenêtre et par IP (réponse `429` au-delà) |
| `game.creation.rate-limit.window` | `PT1M` | Fenêtre du limiteur (durée ISO-8601) |
| `game.max-active-games` | `1000` | Nombre maximal de parties simultanées (`503` au-delà) |
| `game.cleanup.interval` | `PT10M` | Fréquence de la purge des parties inactives |
| `catalog.cache.max-age` | `PT1H` | Durée du `Cache-Control` du catalogue |
| `stats.cache.ttl` | `PT24H` | Durée de vie du cache des statistiques de saison et de l'index de recherche |

Autres comportements :

- Profil `prod` (`SPRING_PROFILES_ACTIVE=prod`) : `CORS_ALLOWED_ORIGINS` est obligatoire (origines séparées par des virgules, ni vide ni `*`), sinon l'API refuse de démarrer. Ce profil ajoute aussi des délais d'expiration de connexion (Tomcat, Hikari) et une taille maximale des en-têtes.
- En `prod`, l'API doit être derrière un reverse proxy dont l'IP est interne (10/8, 172.16/12, 192.168/16, 127/8) : l'IP cliente est lue dans `X-Forwarded-For` (`server.forward-headers-strategy=native`). Sinon, ajuster `server.tomcat.remoteip.internal-proxies`.
- `GET /seasons`, `/teams` et `/formations` sont servis avec `Cache-Control: public` (réponses 2xx uniquement) et un ETag faible. La compression HTTP est active.

## Tests et qualité

```bash
./mvnw verify
```

Les tests couvrent les trois niveaux : unitaires (services, modèles, simulation), contrôleurs (`MockMvc`, `WebMvcTest`) et intégration/bout-en-bout avec une vraie base PostgreSQL via **Testcontainers** — **Docker doit donc être lancé**. Les données de test sont dans `src/test/resources/`.

Le rapport de couverture JaCoCo est généré dans `target/site/jacoco/index.html` (sans seuil bloquant).

## Intégration continue et hooks git

- **CI** (`.github/workflows/ci.yml`, à chaque push et pull request) : garde de règles, vérification que Docker est disponible, `./mvnw -B verify`, puis échec si un test a été ignoré.
- **Garde de règles** (`.githooks/audit-guard.sh`) : détecte les violations de règles sur les fichiers modifiés (`--all` pour tout le dépôt) : `@CrossOrigin`, `System.out`, `SELECT *`, secrets en dur, `catch` vide, `@RequestBody` sans `@Valid`, port Docker exposé hors `127.0.0.1`…. Les exceptions sont déclarées avec leur justification dans `.githooks/audit-guard.allow` (format `règle|chemin|justification`).
- **Hook pre-commit** : lance le garde avant chaque commit. Après un clone, l'activer avec :
  ```bash
  git config core.hooksPath .githooks
  ```
