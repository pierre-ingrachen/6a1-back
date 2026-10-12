package com.takima.backskeleton.game.dao;

import com.takima.backskeleton.game.model.TeamSeason;

import java.util.List;

public interface TeamSeasonSource {
    List<TeamSeason> loadAll();
}
