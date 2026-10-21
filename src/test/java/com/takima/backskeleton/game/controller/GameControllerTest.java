package com.takima.backskeleton.game.controller;

import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameCapacityReachedException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import com.takima.backskeleton.game.model.Formation;
import com.takima.backskeleton.game.model.Game;
import com.takima.backskeleton.game.model.GroupStageResult;
import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.model.RedrawType;
import com.takima.backskeleton.game.model.Round;
import com.takima.backskeleton.game.model.TeamStrength;
import com.takima.backskeleton.game.service.DraftEligibility;
import com.takima.backskeleton.game.service.GameService;
import com.takima.backskeleton.game.service.GameViewMapper;
import com.takima.backskeleton.game.service.TeamSeasonCatalog;
import com.takima.backskeleton.game.support.GameTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GameController.class, FormationController.class})
@Import({GameViewMapper.class, GameControllerTest.CatalogConfig.class})
class GameControllerTest {
    private static final UUID UNKNOWN_GAME_ID = UUID.randomUUID();

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
    private GameService gameService;

    private Game game;

    @BeforeEach
    void setUp() {
        game = GameTestContext.draftService().start(Formation.FOUR_THREE_THREE, 1L);
    }

    @Test
    void findAllFormationsReturnsEveryFormation() throws Exception {
        mockMvc.perform(get("/formations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Formation.values().length))
                .andExpect(jsonPath("$[0].label").value("4-3-3"))
                .andExpect(jsonPath("$[0].positions.length()").value(11));
    }

    @Test
    void createGameReturnsCreatedWithTheDrawnTeam() throws Exception {
        when(gameService.create("4-3-3")).thenReturn(game);

        mockMvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON).content("{\"formation\":\"4-3-3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(game.id().toString()))
                .andExpect(jsonPath("$.status").value("DRAFTING"))
                .andExpect(jsonPath("$.picksRemaining").value(11))
                .andExpect(jsonPath("$.redrawsRemaining").value(3))
                .andExpect(jsonPath("$.slots.length()").value(11))
                .andExpect(jsonPath("$.draw.players").isNotEmpty());
    }

    @Test
    void createGameWithUnknownFormationReturnsBadRequest() throws Exception {
        when(gameService.create("9-9-9")).thenThrow(new UnknownFormationException("9-9-9"));

        mockMvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON).content("{\"formation\":\"9-9-9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown formation 9-9-9"));
    }

    @Test
    void createGameReturnsServiceUnavailableWhenCapacityIsReached() throws Exception {
        when(gameService.create("4-3-3")).thenThrow(new GameCapacityReachedException(1000));

        mockMvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON).content("{\"formation\":\"4-3-3\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createGameWithMalformedBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findGameReturnsOk() throws Exception {
        when(gameService.get(game.id())).thenReturn(game);

        mockMvc.perform(get("/games/" + game.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formation").value("4-3-3"));
    }

    @Test
    void findGameReturnsNotFoundWhenUnknown() throws Exception {
        when(gameService.get(UNKNOWN_GAME_ID)).thenThrow(new GameNotFoundException(UNKNOWN_GAME_ID));

        mockMvc.perform(get("/games/" + UNKNOWN_GAME_ID)).andExpect(status().isNotFound());
    }

    @Test
    void findGameWithMalformedIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/games/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void pickPlayerReturnsOk() throws Exception {
        when(gameService.pick(game.id(), 7, 3)).thenReturn(game);

        mockMvc.perform(post("/games/" + game.id() + "/picks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":7,\"slotIndex\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void pickPlayerReturnsBadRequestWhenInvalid() throws Exception {
        when(gameService.pick(eq(game.id()), anyInt(), anyInt())).thenThrow(new InvalidPickException("nope"));

        mockMvc.perform(post("/games/" + game.id() + "/picks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":7,\"slotIndex\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void movePlayerReturnsOk() throws Exception {
        when(gameService.move(game.id(), 2, 4)).thenReturn(game);

        mockMvc.perform(post("/games/" + game.id() + "/moves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromSlot\":2,\"toSlot\":4}"))
                .andExpect(status().isOk());
    }

    @Test
    void movePlayerReturnsBadRequestWhenInvalid() throws Exception {
        when(gameService.move(eq(game.id()), anyInt(), anyInt())).thenThrow(new InvalidPickException("nope"));

        mockMvc.perform(post("/games/" + game.id() + "/moves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromSlot\":2,\"toSlot\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redrawReturnsOk() throws Exception {
        when(gameService.redraw(game.id(), RedrawType.SEASON)).thenReturn(game);

        mockMvc.perform(post("/games/" + game.id() + "/redraws").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SEASON\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void redrawReturnsConflictWhenQuotaIsExhausted() throws Exception {
        when(gameService.redraw(game.id(), RedrawType.TEAM)).thenThrow(new NoRedrawAvailableException("No redraw left"));

        mockMvc.perform(post("/games/" + game.id() + "/redraws").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TEAM\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void redrawWithUnknownTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/games/" + game.id() + "/redraws").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GALAXY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulationReturnsConflictWhenDraftIsIncomplete() throws Exception {
        when(gameService.simulate(game.id())).thenThrow(new DraftIncompleteException(game.id()));

        mockMvc.perform(post("/games/" + game.id() + "/simulation")).andExpect(status().isConflict());
    }

    @Test
    void simulationReturnsTheCompetitionResult() throws Exception {
        CompetitionResult result = new CompetitionResult(
                new TeamStrength(80, 78), new GroupStageResult(List.of(), List.of(), 2), List.of(), Round.SEMI_FINAL, false);
        when(gameService.simulate(game.id())).thenReturn(result);

        mockMvc.perform(post("/games/" + game.id() + "/simulation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SEMI_FINAL"))
                .andExpect(jsonPath("$.champion").value(false))
                .andExpect(jsonPath("$.groupStage.rank").value(2));
    }

    @Test
    void simulationReturnsNotFoundWhenGameIsGone() throws Exception {
        when(gameService.simulate(UNKNOWN_GAME_ID)).thenThrow(new GameNotFoundException(UNKNOWN_GAME_ID));

        mockMvc.perform(post("/games/" + UNKNOWN_GAME_ID + "/simulation")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /games|{}|formation is required
            /games|{"formation":""}|formation is required
            /games|{"formation":"   "}|formation is required
            /games|{"formation":"123456789012345678901"}|formation must be at most 20 characters
            /games/%s/picks|{"slotIndex":3}|playerId is required
            /games/%s/picks|{"playerId":7}|slotIndex is required
            /games/%s/moves|{"toSlot":3}|fromSlot is required
            /games/%s/moves|{"fromSlot":3}|toSlot is required
            /games/%s/redraws|{}|type is required
            """)
    void invalidRequestBodiesReturnBadRequestWithTheViolation(String pathTemplate, String body, String message) throws Exception {
        mockMvc.perform(post(pathTemplate.formatted(game.id())).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void gameEndpointsAllowFrontOrigin() throws Exception {
        when(gameService.get(game.id())).thenReturn(game);

        mockMvc.perform(get("/games/" + game.id()).header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }
}
