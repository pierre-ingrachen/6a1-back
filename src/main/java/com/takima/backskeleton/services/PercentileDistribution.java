package com.takima.backskeleton.services;

import com.takima.backskeleton.models.Stat;

import java.util.Map;

public record PercentileDistribution(Map<Stat, double[]> totals, Map<Stat, double[]> per90) {
}
