package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.PlayerDto;
import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.DTO.TeamDto;
import com.takima.backskeleton.exceptions.TeamNotFoundException;
import com.takima.backskeleton.services.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {
    private static final Integer UNKNOWN_TEAM_ID = 999;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @Test
    void findAllTeamsReturnsOkWithTeams() throws Exception {
        when(teamService.findAllTeams()).thenReturn(List.of(new TeamDto(13, "Arsenal", "England")));

        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(13))
                .andExpect(jsonPath("$[0].name").value("Arsenal"))
                .andExpect(jsonPath("$[0].country").value("England"));
    }

    @Test
    void findAllTeamsAllowsFrontOrigin() throws Exception {
        when(teamService.findAllTeams()).thenReturn(List.of());

        mockMvc.perform(get("/teams").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void findAllTeamsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(get("/teams").header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findTeamByIdReturnsOkWithTeam() throws Exception {
        when(teamService.findTeamById(13)).thenReturn(new TeamDto(13, "Arsenal", "England"));

        mockMvc.perform(get("/teams/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.name").value("Arsenal"))
                .andExpect(jsonPath("$.country").value("England"));
    }

    @Test
    void findTeamByIdReturnsNotFoundWhenTeamDoesNotExist() throws Exception {
        when(teamService.findTeamById(UNKNOWN_TEAM_ID)).thenThrow(new TeamNotFoundException(UNKNOWN_TEAM_ID));

        mockMvc.perform(get("/teams/" + UNKNOWN_TEAM_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findSeasonsByTeamReturnsOkWithSeasons() throws Exception {
        when(teamService.findSeasonsByTeam(13)).thenReturn(List.of(
                new SeasonDto((short) 2025, "2025/2026"),
                new SeasonDto((short) 2024, "2024/2025")
        ));

        mockMvc.perform(get("/teams/13/seasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startYear").value(2025))
                .andExpect(jsonPath("$[0].label").value("2025/2026"));
    }

    @Test
    void findSeasonsByTeamReturnsNotFoundWhenTeamDoesNotExist() throws Exception {
        when(teamService.findSeasonsByTeam(UNKNOWN_TEAM_ID)).thenThrow(new TeamNotFoundException(UNKNOWN_TEAM_ID));

        mockMvc.perform(get("/teams/" + UNKNOWN_TEAM_ID + "/seasons"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findPlayersByTeamAndSeasonReturnsOkWithPlayers() throws Exception {
        when(teamService.findPlayersByTeamAndSeason(13, (short) 2024)).thenReturn(List.of(
                new PlayerDto(1, "Bukayo Saka", "Forward", (short) 5, (short) 3, new java.math.BigDecimal("7.35"), (short) 8)
        ));

        mockMvc.perform(get("/teams/13/players").param("season", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bukayo Saka"))
                .andExpect(jsonPath("$[0].position").value("Forward"))
                .andExpect(jsonPath("$[0].goals").value(5))
                .andExpect(jsonPath("$[0].assists").value(3))
                .andExpect(jsonPath("$[0].averageRating").value(7.35))
                .andExpect(jsonPath("$[0].matchesPlayed").value(8));
    }

    @Test
    void findPlayersByTeamAndSeasonReturnsNotFoundWhenTeamDoesNotExist() throws Exception {
        when(teamService.findPlayersByTeamAndSeason(UNKNOWN_TEAM_ID, (short) 2024))
                .thenThrow(new TeamNotFoundException(UNKNOWN_TEAM_ID));

        mockMvc.perform(get("/teams/" + UNKNOWN_TEAM_ID + "/players").param("season", "2024"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findPlayersByTeamAndSeasonReturnsBadRequestWithoutSeason() throws Exception {
        mockMvc.perform(get("/teams/13/players"))
                .andExpect(status().isBadRequest());
    }
}
