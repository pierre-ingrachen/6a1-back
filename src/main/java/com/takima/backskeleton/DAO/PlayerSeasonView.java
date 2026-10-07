package com.takima.backskeleton.DAO;

import java.math.BigDecimal;

public interface PlayerSeasonView {
    Integer getId();

    String getName();

    String getPosition();

    Short getGoals();

    Short getAssists();

    BigDecimal getAverageRating();

    Short getMatchesPlayed();
}
