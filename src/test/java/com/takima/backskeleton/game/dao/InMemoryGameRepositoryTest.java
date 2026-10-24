package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGameRepositoryTest {
    private static final Duration TTL = Duration.ofHours(2);

    private final MutableClock clock = new MutableClock();
    private final InMemoryGameRepository repository = new InMemoryGameRepository(clock);

    @Test
    void deleteByIdRemovesTheGame() {
        Game game = repository.save(newGame());

        repository.deleteById(game.id());

        assertThat(repository.findById(game.id())).isEmpty();
    }

    @Test
    void idleGamesAreDeletedAndReturned() {
        Game idle = repository.save(newGame());
        clock.advance(TTL.plusMinutes(1));
        Game recent = repository.save(newGame());

        assertThat(repository.deleteIdleFor(TTL)).containsExactly(idle.id());

        assertThat(repository.findById(idle.id())).isEmpty();
        assertThat(repository.findById(recent.id())).contains(recent);
    }

    @Test
    void onlyIdleGamesMatchingTheFilterAreDeleted() {
        Game kept = repository.save(newGame());
        Game removed = repository.save(newGame());
        clock.advance(TTL.plusMinutes(1));

        assertThat(repository.deleteIdleFor(TTL, game -> game.id().equals(removed.id()))).containsExactly(removed.id());

        assertThat(repository.findById(kept.id())).contains(kept);
        assertThat(repository.findById(removed.id())).isEmpty();
    }

    @Test
    void readingAGameKeepsItAlive() {
        Game game = repository.save(newGame());
        clock.advance(TTL.minusMinutes(1));
        repository.findById(game.id());
        clock.advance(TTL.minusMinutes(1));

        assertThat(repository.deleteIdleFor(TTL)).isEmpty();
        assertThat(repository.findById(game.id())).contains(game);
    }

    @Test
    void countReflectsStoredGames() {
        Game first = repository.save(newGame());
        repository.save(newGame());
        repository.deleteById(first.id());

        assertThat(repository.count()).isEqualTo(1);
    }

    private Game newGame() {
        return Game.create(UUID.randomUUID(), 1L, Formation.FOUR_THREE_THREE);
    }
}
