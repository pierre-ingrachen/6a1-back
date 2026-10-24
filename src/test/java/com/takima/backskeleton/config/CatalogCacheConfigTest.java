package com.takima.backskeleton.config;

import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.DTO.TeamDto;
import com.takima.backskeleton.controllers.SeasonController;
import com.takima.backskeleton.controllers.TeamController;
import com.takima.backskeleton.exceptions.GlobalExceptionHandler;
import com.takima.backskeleton.game.controller.FormationController;
import com.takima.backskeleton.game.controller.GameController;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.service.DraftEligibility;
import com.takima.backskeleton.game.service.GameCreationRateLimiter;
import com.takima.backskeleton.game.service.GameService;
import com.takima.backskeleton.game.service.GameViewMapper;
import com.takima.backskeleton.game.service.TeamSeasonCatalog;
import com.takima.backskeleton.game.support.GameTestContext;
import com.takima.backskeleton.services.SeasonService;
import com.takima.backskeleton.services.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SeasonController.class, TeamController.class, FormationController.class, GameController.class})
@Import({CatalogCacheConfig.class, GlobalExceptionHandler.class, GameViewMapper.class, CatalogCacheConfigTest.CatalogConfig.class})
class CatalogCacheConfigTest {
    @TestConfiguration
    static class CatalogConfig {
        @Bean
        TeamSeasonCatalog catalog() {
            return GameTestContext.catalog();
        }

        @Bean
        DraftEligibility eligibility() {
            return GameTestContext.eligibility();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeasonService seasonService;

    @MockBean
    private TeamService teamService;

    @MockBean
    private GameService gameService;

    @MockBean
    private GameCreationRateLimiter gameCreationRateLimiter;

    @Test
    void catalogEndpointsAreCacheableForOneHour() throws Exception {
        when(seasonService.findAllSeasons()).thenReturn(List.of(new SeasonDto((short) 2024, "2024/2025")));
        when(teamService.findAllTeams()).thenReturn(List.of(new TeamDto(13, "Arsenal", "England")));

        for (String path : List.of("/seasons", "/teams", "/formations")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600, public"))
                    .andExpect(header().string(HttpHeaders.ETAG, startsWith("W/\"")));
        }
    }

    @Test
    void unchangedCatalogAnswersNotModifiedToAConditionalRequest() throws Exception {
        when(seasonService.findAllSeasons()).thenReturn(List.of(new SeasonDto((short) 2024, "2024/2025")));
        MvcResult first = mockMvc.perform(get("/seasons")).andReturn();
        String etag = first.getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/seasons").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void gameEndpointsAreNeverCached() throws Exception {
        Game game = GameTestContext.draftService().start(Formation.FOUR_THREE_THREE, 1L);
        when(gameService.get(game.id())).thenReturn(game);
        when(gameService.create("4-3-3")).thenReturn(game);

        mockMvc.perform(get("/games/" + game.id()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()))
                .andExpect(header().string(HttpHeaders.ETAG, nullValue()));
        mockMvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON).content("{\"formation\":\"4-3-3\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()))
                .andExpect(header().string(HttpHeaders.ETAG, nullValue()));
        mockMvc.perform(get("/games/" + UUID.randomUUID()))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()));
    }

    @Test
    void failedCatalogRequestsAreNotCacheable() throws Exception {
        when(teamService.findAllTeams()).thenThrow(new IllegalStateException("database down"));

        mockMvc.perform(get("/teams"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()))
                .andExpect(header().string(HttpHeaders.ETAG, nullValue()));
    }

    @Test
    void otherTeamEndpointsAreNotCached() throws Exception {
        when(teamService.findSeasonsByTeam(13)).thenReturn(List.of());

        mockMvc.perform(get("/teams/13/seasons"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, nullValue()));
    }
}
