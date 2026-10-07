package com.takima.backskeleton.models;

public enum StatCategory {
    GOALS("Buts"),
    SHOTS("Tirs"),
    CREATION("Création"),
    PASSING("Passes"),
    CROSSES_AND_SET_PIECES("Centres et coups de pied arrêtés"),
    DRIBBLING("Dribbles et conservation"),
    DEFENDING("Défense"),
    AERIAL_DUELS("Duels aériens"),
    DISCIPLINE("Discipline"),
    GOALKEEPING("Gardien"),
    PLAYING_TIME("Temps de jeu");

    private final String label;

    StatCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
