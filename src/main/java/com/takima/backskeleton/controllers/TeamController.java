package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.PlayerDto;
import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.DTO.TeamDto;
import com.takima.backskeleton.services.TeamService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("teams")
@RestController
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamDto> findAllTeams() {
        return teamService.findAllTeams();
    }

    @GetMapping("/{teamId}")
    public TeamDto findTeamById(@PathVariable Integer teamId) {
        return teamService.findTeamById(teamId);
    }

    @GetMapping("/{teamId}/seasons")
    public List<SeasonDto> findSeasonsByTeam(@PathVariable Integer teamId) {
        return teamService.findSeasonsByTeam(teamId);
    }

    @GetMapping("/{teamId}/players")
    public List<PlayerDto> findPlayersByTeamAndSeason(@PathVariable Integer teamId, @RequestParam Short season) {
        return teamService.findPlayersByTeamAndSeason(teamId, season);
    }
}
