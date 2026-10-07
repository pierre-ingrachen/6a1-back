package com.takima.backskeleton.DTO;

import java.util.List;

public record StatCategoryDto(String key, String label, List<StatDto> stats) {
}
