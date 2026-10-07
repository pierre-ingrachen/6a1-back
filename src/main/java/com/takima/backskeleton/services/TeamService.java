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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {
    private final TeamDao teamDao;
    private final SeasonDao seasonDao;
    private final PlayerDao playerDao;

    public TeamService(TeamDao teamDao, SeasonDao seasonDao, PlayerDao playerDao) {
        this.teamDao = teamDao;
        this.seasonDao = seasonDao;
        this.playerDao = playerDao;
    }

    public List<TeamDto> findAllTeams() {
        return teamDao.findAll().stream()
                .map(this::toTeamDto)
                .toList();
    }

    public TeamDto findTeamById(Integer teamId) {
        return teamDao.findById(teamId)
                .map(this::toTeamDto)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
    }

    public List<SeasonDto> findSeasonsByTeam(Integer teamId) {
        ensureTeamExists(teamId);
        return seasonDao.findByTeamId(teamId).stream()
                .map(this::toSeasonDto)
                .toList();
    }

    public List<PlayerDto> findPlayersByTeamAndSeason(Integer teamId, Short season) {
        ensureTeamExists(teamId);
        return playerDao.findByTeamIdAndSeason(teamId, season).stream()
                .map(this::toPlayerDto)
                .toList();
    }

    private void ensureTeamExists(Integer teamId) {
        if (!teamDao.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
    }

    private TeamDto toTeamDto(Team team) {
        return new TeamDto(team.getId(), team.getName(), team.getCountry());
    }

    private SeasonDto toSeasonDto(Season season) {
        return new SeasonDto(season.getStartYear(), season.getLabel());
    }

    private PlayerDto toPlayerDto(PlayerSeasonView player) {
        return new PlayerDto(player.getId(), player.getName(), player.getPosition(), player.getGoals(), player.getAssists(), player.getAverageRating());
    }
}
