package com.takima.backskeleton.DTO;

public record StatDto(String key, String label, boolean detail, boolean lowerIsBetter, StatValueDto total, StatValueDto per90) {
}
