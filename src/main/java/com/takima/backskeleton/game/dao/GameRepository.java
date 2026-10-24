package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.Game;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public interface GameRepository {
    Game save(Game game);

    Optional<Game> findById(UUID id);

    void deleteById(UUID id);

    int count();

    List<UUID> deleteIdleFor(Duration idleDuration, Predicate<Game> filter);

    default List<UUID> deleteIdleFor(Duration idleDuration) {
        return deleteIdleFor(idleDuration, game -> true);
    }
}
