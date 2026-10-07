package com.takima.backskeleton.DTO;

import java.math.BigDecimal;

public record PlayerDto(Integer id, String name, String position, Short goals, Short assists, BigDecimal averageRating) {
}
