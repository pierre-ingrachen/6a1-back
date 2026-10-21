package com.takima.backskeleton.services;

import java.util.Map;

public record SeasonSnapshot(Map<Integer, SeasonPlayerStats> playersById, PercentileDistribution distribution) {
}
