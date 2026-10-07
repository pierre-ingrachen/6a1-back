package com.takima.backskeleton.DTO;

import java.util.List;

public record PlayerSearchResultDto(Integer id, String name, String position, String teamName, List<PlayerSeasonTeamDto> seasons) {
}
