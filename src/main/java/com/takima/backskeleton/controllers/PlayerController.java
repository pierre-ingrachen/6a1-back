package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.services.PlayerService;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequestMapping("players")
@RestController
public class PlayerController {
    static final int MAXIMUM_QUERY_LENGTH = 100;

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/search")
    public List<PlayerSearchResultDto> searchPlayers(@RequestParam @Size(max = MAXIMUM_QUERY_LENGTH, message = "query must be at most 100 characters") String query, @RequestParam(required = false) @SeasonYear Short season) {
        return playerService.searchPlayers(season, query);
    }

    @GetMapping("/{playerId}/stats")
    public PlayerStatsDto findPlayerStats(@PathVariable Integer playerId, @RequestParam @SeasonYear Short season) {
        return playerService.findPlayerStats(playerId, season);
    }
}
