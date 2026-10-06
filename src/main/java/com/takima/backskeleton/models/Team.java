package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipe")
public class Team {
    @Id
    @Column(name = "equipe_id")
    private Integer id;
    @Column(name = "nom")
    private String name;
    @Column(name = "pays")
    private String country;

    public Team() {
    }

    public Team(Integer id, String name, String country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }
}
