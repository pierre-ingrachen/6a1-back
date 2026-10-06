package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "joueur")
public class Player {
    @Id
    @Column(name = "joueur_id")
    private Integer id;
    @Column(name = "nom")
    private String name;
    @Column(name = "poste")
    private String position;
    @Column(name = "taille_cm")
    private Short heightCm;
    @Column(name = "poids_kg")
    private Short weightKg;

    public Player() {
    }

    public Player(Integer id, String name, String position, Short heightCm, Short weightKg) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public Short getHeightCm() {
        return heightCm;
    }

    public Short getWeightKg() {
        return weightKg;
    }
}
