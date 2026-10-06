package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerDao extends JpaRepository<Player, Integer> {
    @Query(value = """
            SELECT j.* FROM joueur j
            JOIN stats_joueur_saison s ON s.joueur_id = j.joueur_id
            WHERE s.equipe_id = :teamId AND s.annee_debut = :season
            ORDER BY j.nom
            """, nativeQuery = true)
    List<Player> findByTeamIdAndSeason(Integer teamId, Short season);
}
