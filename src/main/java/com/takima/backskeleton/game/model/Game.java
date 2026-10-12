package com.takima.backskeleton.game.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public record Game(
        UUID id,
        long seed,
        Formation formation,
        TeamSeasonKey currentDraw,
        Map<Integer, PickedPlayer> picks,
        Set<TeamSeasonKey> seenDraws,
        int redrawsLeft,
        int drawCount,
        CompetitionResult result
) {
    public static final int INITIAL_REDRAWS = 3;

    public static Game create(UUID id, long seed, Formation formation) {
        return new Game(id, seed, formation, null, Map.of(), Set.of(), INITIAL_REDRAWS, 0, null);
    }

    public boolean isDraftComplete() {
        return picks.size() == Formation.SQUAD_SIZE;
    }

    public boolean isCompleted() {
        return result != null;
    }

    public int picksRemaining() {
        return Formation.SQUAD_SIZE - picks.size();
    }

    public List<Integer> openSlotIndexes() {
        return IntStream.range(0, formation.slots().size())
                .filter(index -> !picks.containsKey(index))
                .boxed()
                .toList();
    }

    public boolean hasPicked(int playerId) {
        return picks.values().stream().anyMatch(pick -> pick.playerId() == playerId);
    }

    public Game withDraw(TeamSeasonKey draw) {
        Set<TeamSeasonKey> seen = new HashSet<>(seenDraws);
        seen.add(draw);
        return new Game(id, seed, formation, draw, picks, Set.copyOf(seen), redrawsLeft, drawCount + 1, result);
    }

    public Game withRedrawUsed() {
        return new Game(id, seed, formation, currentDraw, picks, seenDraws, redrawsLeft - 1, drawCount, result);
    }

    public Game withPick(int slotIndex, PickedPlayer player) {
        Map<Integer, PickedPlayer> updated = new HashMap<>(picks);
        updated.put(slotIndex, player);
        return new Game(id, seed, formation, currentDraw, Map.copyOf(updated), seenDraws, redrawsLeft, drawCount, result);
    }

    public Game withMove(int fromSlot, int toSlot) {
        Map<Integer, PickedPlayer> updated = new HashMap<>(picks);
        PickedPlayer moved = updated.remove(fromSlot);
        PickedPlayer displaced = updated.put(toSlot, moved);
        if (displaced != null) {
            updated.put(fromSlot, displaced);
        }
        return new Game(id, seed, formation, currentDraw, Map.copyOf(updated), seenDraws, redrawsLeft, drawCount, result);
    }

    public Game withoutDraw() {
        return new Game(id, seed, formation, null, picks, seenDraws, redrawsLeft, drawCount, result);
    }

    public Game withResult(CompetitionResult competitionResult) {
        return new Game(id, seed, formation, currentDraw, picks, seenDraws, redrawsLeft, drawCount, competitionResult);
    }
}
