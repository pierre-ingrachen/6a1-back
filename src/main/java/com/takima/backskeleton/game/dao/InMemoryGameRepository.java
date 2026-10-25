package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.Game;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGameRepository implements GameRepository {
    private record StoredGame(Game game, Instant lastAccess) {
        StoredGame touchedAt(Instant instant) {
            return new StoredGame(game, instant);
        }
    }

    private final Map<UUID, StoredGame> games = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryGameRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Game save(Game game) {
        games.put(game.id(), new StoredGame(game, clock.instant()));
        return game;
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.computeIfPresent(id, (key, stored) -> stored.touchedAt(clock.instant())))
                .map(StoredGame::game);
    }

    @Override
    public int count() {
        return games.size();
    }

    @Override
    public void deleteById(UUID id) {
        games.remove(id);
    }

    @Override
    public List<UUID> deleteIdleFor(Duration idleDuration, Predicate<Game> filter) {
        Instant threshold = clock.instant().minus(idleDuration);
        List<UUID> deletedIds = new ArrayList<>();
        games.keySet().forEach(id -> games.computeIfPresent(id, (key, stored) -> {
            if (stored.lastAccess().isBefore(threshold) && filter.test(stored.game())) {
                deletedIds.add(key);
                return null;
            }
            return stored;
        }));
        return List.copyOf(deletedIds);
    }
}
