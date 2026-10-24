package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.dto.GameView;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class GameViewMapperTest {
    private static final int SEARCHED_SEEDS = 500;

    private final TeamDrawService teamDrawService = GameTestContext.drawService();
    private final DraftService draftService = GameTestContext.draftService();
    private final GameViewMapper mapper = new GameViewMapper(
            GameTestContext.catalog(), GameTestContext.eligibility(), teamDrawService);

    @Test
    void exposesThatNoOtherSeasonIsAvailableWhileOtherTeamsAre() {
        Game game = gameWhere(candidate -> !teamDrawService.canRedraw(candidate, RedrawType.SEASON)
                && teamDrawService.canRedraw(candidate, RedrawType.TEAM));

        GameView.DrawView draw = mapper.toView(game).draw();

        assertThat(draw.seasonRedrawAvailable()).isFalse();
        assertThat(draw.teamRedrawAvailable()).isTrue();
    }

    @Test
    void exposesThatOtherSeasonsAndTeamsAreAvailable() {
        Game game = gameWhere(candidate -> teamDrawService.canRedraw(candidate, RedrawType.SEASON)
                && teamDrawService.canRedraw(candidate, RedrawType.TEAM));

        GameView.DrawView draw = mapper.toView(game).draw();

        assertThat(draw.seasonRedrawAvailable()).isTrue();
        assertThat(draw.teamRedrawAvailable()).isTrue();
    }

    private Game gameWhere(Predicate<Game> condition) {
        return LongStream.range(0, SEARCHED_SEEDS)
                .mapToObj(seed -> draftService.start(Formation.FOUR_THREE_THREE, seed))
                .filter(condition)
                .findFirst().orElseThrow();
    }
}
