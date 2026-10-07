package com.takima.backskeleton.DTO;

import java.util.List;

public record PlayerStatsDto(
        Integer id,
        String name,
        String position,
        Short heightCm,
        Short weightKg,
        String teamName,
        boolean teamEstimated,
        Short rating,
        Double averageRating,
        Integer minutes,
        List<StatCategoryDto> categories
) {
}
