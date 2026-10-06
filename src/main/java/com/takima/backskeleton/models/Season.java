package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "saison")
public class Season {
    @Id
    @Column(name = "annee_debut")
    private Short startYear;
    @Column(name = "libelle")
    private String label;

    public Season() {
    }

    public Season(Short startYear, String label) {
        this.startYear = startYear;
        this.label = label;
    }

    public Short getStartYear() {
        return startYear;
    }

    public String getLabel() {
        return label;
    }
}
