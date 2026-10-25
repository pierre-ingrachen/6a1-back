package com.takima.backskeleton.exceptions;

import com.takima.backskeleton.game.controller.GameExceptionHandler;
import com.takima.backskeleton.game.exception.DraftCompletedException;
import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameCapacityReachedException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExceptionHandlersTest {
    private static final UUID GAME_ID = UUID.randomUUID();
    private static final String INTERNAL_DETAIL = "jdbc:postgresql://secret-host/db";

    @RestController
    static class ThrowingController {
        static volatile Supplier<RuntimeException> failure = () -> new IllegalStateException(INTERNAL_DETAIL);

        @GetMapping("/fail")
        String fail() {
            throw failure.get();
        }

        @GetMapping("/param")
        String param(@RequestParam String required) {
            return required;
        }

        @PostMapping("/body")
        String body(@RequestBody Payload payload) {
            return payload.name();
        }
    }

    record Payload(String name) {
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(), new GameExceptionHandler(), new CatalogExceptionHandler())
                .build();
    }

    static Stream<Object[]> handledExceptions() {
        return Stream.of(
                new Object[]{new GameNotFoundException(GAME_ID), 404, "Game " + GAME_ID + " not found"},
                new Object[]{new InvalidPickException("nope"), 400, "nope"},
                new Object[]{new UnknownFormationException("9-9-9"), 400, "Unknown formation 9-9-9"},
                new Object[]{new DraftCompletedException(GAME_ID), 409, "Draft of game " + GAME_ID + " is already complete"},
                new Object[]{new DraftIncompleteException(GAME_ID), 409, "Draft of game " + GAME_ID + " is not complete"},
                new Object[]{new NoRedrawAvailableException("No redraw left"), 409, "No redraw left"},
                new Object[]{new GameCapacityReachedException(5), 503, "Too many active games (5), try again later"},
                new Object[]{new TeamNotFoundException(7), 404, "Team 7 not found"},
                new Object[]{new PlayerStatsNotFoundException(3, (short) 2025), 404, "No stats for player 3 in season 2025"}
        );
    }

    @ParameterizedTest
    @MethodSource("handledExceptions")
    void domainExceptionsAreMappedToStatusAndJsonMessage(RuntimeException exception, int status, String message) throws Exception {
        ThrowingController.failure = () -> exception;

        mockMvc.perform(get("/fail"))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void unexpectedExceptionReturnsGenericInternalServerError() throws Exception {
        ThrowingController.failure = () -> new IllegalStateException(INTERNAL_DETAIL);

        mockMvc.perform(get("/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE))
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void missingRequestParameterReturnsBadRequestWithMessage() throws Exception {
        mockMvc.perform(get("/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    void malformedBodyReturnsBadRequestWithMessage() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }
}
