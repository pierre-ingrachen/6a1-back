package com.takima.backskeleton.game.service;

import com.takima.backskeleton.game.model.FieldPosition;
import com.takima.backskeleton.game.model.Line;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.SeasonPlayer;
import com.takima.backskeleton.game.model.TeamSeason;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TeamSeasonCatalogTest {
    private final TeamSeasonCatalog catalog = GameTestContext.catalog();

    @Test
    void drawableTeamSeasonsAllReachedTheRoundOfSixteen() {
        assertThat(catalog.drawableTeamSeasons())
                .allMatch(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16));
    }

    @Test
    void drawableTeamSeasonsAllHaveAStarWithEnoughMinutes() {
        assertThat(catalog.drawableTeamSeasons())
                .allMatch(teamSeason -> teamSeason.players().stream()
                        .anyMatch(player -> player.minutes() >= GameBalance.MIN_DRAFT_MINUTES
                                && player.rating() >= GameBalance.MIN_DRAWABLE_STAR_RATING));
    }

    @Test
    void everyQualifyingTeamSeasonWithAStarIsDrawable() {
        List<TeamSeason> drawable = catalog.drawableTeamSeasons();

        assertThat(catalog.teamSeasons())
                .filteredOn(teamSeason -> teamSeason.roundReached().hasPlayed(Round.ROUND_OF_16))
                .filteredOn(teamSeason -> teamSeason.players().stream()
                        .anyMatch(player -> player.minutes() >= GameBalance.MIN_DRAFT_MINUTES
                                && player.rating() >= GameBalance.MIN_DRAWABLE_STAR_RATING))
                .allMatch(drawable::contains);
    }

    @Test
    void starRatingIsOnlyCountedFromTheMinimumDraftMinutes() {
        TeamSeasonCatalog custom = catalogOf(
                teamSeasonOf(1, Round.QUARTER_FINAL, playerOf(1, 87, GameBalance.MIN_DRAFT_MINUTES - 1)),
                teamSeasonOf(2, Round.QUARTER_FINAL, playerOf(2, 87, GameBalance.MIN_DRAFT_MINUTES)),
                teamSeasonOf(3, Round.QUARTER_FINAL, playerOf(3, GameBalance.MIN_DRAWABLE_STAR_RATING - 1, 2000)));

        assertThat(custom.drawableTeamSeasons()).extracting(TeamSeason::teamId).containsExactly(2);
    }

    @Test
    void groupStageTeamSeasonWithAStarIsNotDrawable() {
        TeamSeasonCatalog custom = catalogOf(
                teamSeasonOf(1, Round.GROUP_STAGE, playerOf(1, 95, 2000)),
                teamSeasonOf(2, Round.ROUND_OF_16, playerOf(2, 95, 2000)));

        assertThat(custom.drawableTeamSeasons()).extracting(TeamSeason::teamId).containsExactly(2);
    }

    private TeamSeasonCatalog catalogOf(TeamSeason... teamSeasons) {
        return new TeamSeasonCatalog(() -> List.of(teamSeasons), GameTestContext.strengthService());
    }

    private TeamSeason teamSeasonOf(int teamId, Round round, SeasonPlayer player) {
        return new TeamSeason(teamId, "Team " + teamId, "Country", 2020, "2020/2021", round, List.of(player));
    }

    private SeasonPlayer playerOf(int playerId, int rating, int minutes) {
        return new SeasonPlayer(playerId, "Player " + playerId, rating, Line.ATTACKER, Set.of(FieldPosition.ST), minutes);
    }
}
