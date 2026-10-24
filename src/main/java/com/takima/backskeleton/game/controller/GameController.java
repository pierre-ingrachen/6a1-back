package com.takima.backskeleton.game.controller;

import com.takima.backskeleton.game.dto.CreateGameRequest;
import com.takima.backskeleton.game.dto.GameView;
import com.takima.backskeleton.game.dto.MoveRequest;
import com.takima.backskeleton.game.dto.PickRequest;
import com.takima.backskeleton.game.dto.RedrawRequest;
import com.takima.backskeleton.game.model.CompetitionResult;
import com.takima.backskeleton.game.service.GameService;
import com.takima.backskeleton.game.service.GameViewMapper;
import com.takima.backskeleton.game.service.GameCreationRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequestMapping("games")
@RestController
public class GameController {
    private final GameService gameService;
    private final GameViewMapper gameViewMapper;
    private final GameCreationRateLimiter gameCreationRateLimiter;

    public GameController(GameService gameService, GameViewMapper gameViewMapper,
                          GameCreationRateLimiter gameCreationRateLimiter) {
        this.gameService = gameService;
        this.gameViewMapper = gameViewMapper;
        this.gameCreationRateLimiter = gameCreationRateLimiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameView createGame(@Valid @RequestBody CreateGameRequest request, HttpServletRequest httpRequest) {
        gameCreationRateLimiter.acquire(httpRequest.getRemoteAddr());
        return gameViewMapper.toView(gameService.create(request.formation()));
    }

    @GetMapping("/{gameId}")
    public GameView findGame(@PathVariable UUID gameId) {
        return gameViewMapper.toView(gameService.get(gameId));
    }

    @PostMapping("/{gameId}/picks")
    public GameView pickPlayer(@PathVariable UUID gameId, @Valid @RequestBody PickRequest request) {
        return gameViewMapper.toView(gameService.pick(gameId, request.playerId(), request.slotIndex()));
    }

    @PostMapping("/{gameId}/moves")
    public GameView movePlayer(@PathVariable UUID gameId, @Valid @RequestBody MoveRequest request) {
        return gameViewMapper.toView(gameService.move(gameId, request.fromSlot(), request.toSlot()));
    }

    @PostMapping("/{gameId}/redraws")
    public GameView redraw(@PathVariable UUID gameId, @Valid @RequestBody RedrawRequest request) {
        return gameViewMapper.toView(gameService.redraw(gameId, request.type()));
    }

    @PostMapping("/{gameId}/simulation")
    public CompetitionResult simulateCompetition(@PathVariable UUID gameId) {
        return gameService.simulate(gameId);
    }
}
