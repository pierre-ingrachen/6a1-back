package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.PlayerSearchResultDto;
import com.takima.backskeleton.DTO.PlayerStatsDto;
import com.takima.backskeleton.services.PlayerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("players")
@RestController
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/search")
    public List<PlayerSearchResultDto> searchPlayers(@RequestParam Short season, @RequestParam String query) {
        return playerService.searchPlayers(season, query);
    }

    @GetMapping("/{playerId}/stats")
    public PlayerStatsDto findPlayerStats(@PathVariable Integer playerId, @RequestParam Short season) {
        return playerService.findPlayerStats(playerId, season);
    }
}
