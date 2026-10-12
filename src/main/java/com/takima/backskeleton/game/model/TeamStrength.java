package com.takima.backskeleton.game.model;

public record TeamStrength(double attack, double defense) {
    public double overall() {
        return (attack + defense) / 2;
    }
}
