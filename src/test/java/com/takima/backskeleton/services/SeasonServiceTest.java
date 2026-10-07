package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.SeasonDao;
import com.takima.backskeleton.DTO.SeasonDto;
import com.takima.backskeleton.models.Season;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {
    @Mock
    private SeasonDao seasonDao;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    void findAllSeasonsReturnsSeasonsFromMostRecent() {
        when(seasonDao.findAll(Sort.by(Sort.Direction.DESC, "startYear"))).thenReturn(List.of(
                new Season((short) 2025, "2025/2026"),
                new Season((short) 2024, "2024/2025")
        ));

        assertThat(seasonService.findAllSeasons()).containsExactly(
                new SeasonDto((short) 2025, "2025/2026"),
                new SeasonDto((short) 2024, "2024/2025")
        );
    }
}
