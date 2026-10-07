package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerSeasonTeamDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.DTO.StatCategoryDto;
import com.takima.backskeleton.DTO.StatDto;
import com.takima.backskeleton.DTO.StatValueDto;
import com.takima.backskeleton.exceptions.PlayerStatsNotFoundException;
import com.takima.backskeleton.services.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {
    private static final Short SEASON = 2025;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    @Test
    void searchPlayersReturnsOkWithMatchingPlayers() throws Exception {
        when(playerService.searchPlayers("mba"))
                .thenReturn(List.of(new PlayerSearchResultDto(123, "Kylian Mbappé", "Forward", "Real Madrid", List.of(new PlayerSeasonTeamDto((short) 2025, "Real Madrid"), new PlayerSeasonTeamDto((short) 2024, "PSG")))));

        mockMvc.perform(get("/players/search").param("query", "mba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(123))
                .andExpect(jsonPath("$[0].name").value("Kylian Mbappé"))
                .andExpect(jsonPath("$[0].teamName").value("Real Madrid"))
                .andExpect(jsonPath("$[0].seasons.length()").value(2));
    }

    @Test
    void searchPlayersReturnsBadRequestWithoutQuery() throws Exception {
        mockMvc.perform(get("/players/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findPlayerStatsReturnsOkWithStats() throws Exception {
        StatDto goals = new StatDto("GOALS_SCORED", "Buts", false, false,
                new StatValueDto(6.0, 92, 8), new StatValueDto(1.0, 88, 12));
        when(playerService.findPlayerStats(123, SEASON)).thenReturn(new PlayerStatsDto(
                123, "Kylian Mbappé", "Forward", (short) 178, null, "Real Madrid", true, (short) 92, 7.8, 540,
                List.of(new StatCategoryDto("GOALS", "Buts", List.of(goals)))));

        mockMvc.perform(get("/players/123/stats").param("season", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kylian Mbappé"))
                .andExpect(jsonPath("$.teamEstimated").value(true))
                .andExpect(jsonPath("$.weightKg").isEmpty())
                .andExpect(jsonPath("$.categories[0].key").value("GOALS"))
                .andExpect(jsonPath("$.categories[0].stats[0].total.value").value(6.0))
                .andExpect(jsonPath("$.categories[0].stats[0].total.topPercent").value(8))
                .andExpect(jsonPath("$.categories[0].stats[0].per90.percentile").value(88));
    }

    @Test
    void findPlayerStatsReturnsNotFoundWhenPlayerHasNoStatsInSeason() throws Exception {
        when(playerService.findPlayerStats(999, SEASON)).thenThrow(new PlayerStatsNotFoundException(999, SEASON));

        mockMvc.perform(get("/players/999/stats").param("season", "2025"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findPlayerStatsReturnsBadRequestWithoutSeason() throws Exception {
        mockMvc.perform(get("/players/123/stats"))
                .andExpect(status().isBadRequest());
    }
}
