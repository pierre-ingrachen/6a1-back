package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonDao extends JpaRepository<Season, Short> {
    @Query(value = """
            SELECT DISTINCT sa.* FROM saison sa
            JOIN stats_joueur_saison s ON s.annee_debut = sa.annee_debut
            WHERE s.equipe_id = :teamId
            ORDER BY sa.annee_debut DESC
            """, nativeQuery = true)
    List<Season> findByTeamId(Integer teamId);
}
