package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamDao extends JpaRepository<Team, Integer> {
}
