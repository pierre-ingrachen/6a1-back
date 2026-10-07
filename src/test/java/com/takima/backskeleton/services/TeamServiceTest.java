package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PlayerDao;
import com.takima.backskeleton.DAO.PlayerSeasonView;
import com.takima.backskeleton.DAO.SeasonDao;
import com.takima.backskeleton.DAO.TeamDao;
import com.takima.backskeleton.DTO.PlayerDto;
import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.DTO.TeamDto;
import com.takima.backskeleton.exceptions.TeamNotFoundException;
import com.takima.backskeleton.models.Season;
import com.takima.backskeleton.models.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    private static final Integer ARSENAL_ID = 13;
    private static final Integer UNKNOWN_TEAM_ID = 999;
    private static final Short SEASON_2024 = 2024;

    @Mock
    private TeamDao teamDao;

    @Mock
    private SeasonDao seasonDao;

    @Mock
    private PlayerDao playerDao;

    @InjectMocks
    private TeamService teamService;

    @Test
    void findAllTeamsConvertsEntitiesToDtos() {
        when(teamDao.findAll()).thenReturn(List.of(
                new Team(13, "Arsenal", "England"),
                new Team(37, "Bayern", "Germany")
        ));

        List<TeamDto> teams = teamService.findAllTeams();

        assertThat(teams).containsExactly(
                new TeamDto(13, "Arsenal", "England"),
                new TeamDto(37, "Bayern", "Germany")
        );
    }

    @Test
    void findAllTeamsReturnsEmptyListWhenNoTeam() {
        when(teamDao.findAll()).thenReturn(List.of());

        assertThat(teamService.findAllTeams()).isEmpty();
    }

    @Test
    void findTeamByIdReturnsTeam() {
        when(teamDao.findById(ARSENAL_ID)).thenReturn(Optional.of(new Team(ARSENAL_ID, "Arsenal", "England")));

        assertThat(teamService.findTeamById(ARSENAL_ID)).isEqualTo(new TeamDto(ARSENAL_ID, "Arsenal", "England"));
    }

    @Test
    void findTeamByIdThrowsWhenTeamNotFound() {
        when(teamDao.findById(UNKNOWN_TEAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.findTeamById(UNKNOWN_TEAM_ID))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void findSeasonsByTeamConvertsEntitiesToDtos() {
        when(teamDao.existsById(ARSENAL_ID)).thenReturn(true);
        when(seasonDao.findByTeamId(ARSENAL_ID)).thenReturn(List.of(
                new Season((short) 2025, "2025/2026"),
                new Season((short) 2024, "2024/2025")
        ));

        List<SeasonDto> seasons = teamService.findSeasonsByTeam(ARSENAL_ID);

        assertThat(seasons).containsExactly(
                new SeasonDto((short) 2025, "2025/2026"),
                new SeasonDto((short) 2024, "2024/2025")
        );
    }

    @Test
    void findSeasonsByTeamThrowsWhenTeamNotFound() {
        when(teamDao.existsById(UNKNOWN_TEAM_ID)).thenReturn(false);

        assertThatThrownBy(() -> teamService.findSeasonsByTeam(UNKNOWN_TEAM_ID))
                .isInstanceOf(TeamNotFoundException.class);
        verify(seasonDao, never()).findByTeamId(any());
    }

    @Test
    void findPlayersByTeamAndSeasonConvertsEntitiesToDtosKeepingUnknownValues() {
        when(teamDao.existsById(ARSENAL_ID)).thenReturn(true);
        when(playerDao.findByTeamIdAndSeason(ARSENAL_ID, SEASON_2024)).thenReturn(List.of(
                view(1, "Bukayo Saka", "Forward", (short) 5, (short) 3, new BigDecimal("7.35")),
                view(2, "Unknown Keeper", "Goalkeeper", null, null, null)
        ));

        List<PlayerDto> players = teamService.findPlayersByTeamAndSeason(ARSENAL_ID, SEASON_2024);

        assertThat(players).containsExactly(
                new PlayerDto(1, "Bukayo Saka", "Forward", (short) 5, (short) 3, new BigDecimal("7.35")),
                new PlayerDto(2, "Unknown Keeper", "Goalkeeper", null, null, null)
        );
    }

    @Test
    void findPlayersByTeamAndSeasonThrowsWhenTeamNotFound() {
        when(teamDao.existsById(UNKNOWN_TEAM_ID)).thenReturn(false);

        assertThatThrownBy(() -> teamService.findPlayersByTeamAndSeason(UNKNOWN_TEAM_ID, SEASON_2024))
                .isInstanceOf(TeamNotFoundException.class);
        verify(playerDao, never()).findByTeamIdAndSeason(any(), any());
    }

    private static PlayerSeasonView view(Integer id, String name, String position, Short goals, Short assists, BigDecimal averageRating) {
        return new PlayerSeasonView() {
            public Integer getId() { return id; }
            public String getName() { return name; }
            public String getPosition() { return position; }
            public Short getGoals() { return goals; }
            public Short getAssists() { return assists; }
            public BigDecimal getAverageRating() { return averageRating; }
        };
    }
}
