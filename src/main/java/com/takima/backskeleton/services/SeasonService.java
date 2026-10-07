package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.SeasonDao;
import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.models.Season;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonService {
    private final SeasonDao seasonDao;

    public SeasonService(SeasonDao seasonDao) {
        this.seasonDao = seasonDao;
    }

    public List<SeasonDto> findAllSeasons() {
        return seasonDao.findAll(Sort.by(Sort.Direction.DESC, "startYear")).stream()
                .map(this::toSeasonDto)
                .toList();
    }

    private SeasonDto toSeasonDto(Season season) {
        return new SeasonDto(season.getStartYear(), season.getLabel());
    }
}
