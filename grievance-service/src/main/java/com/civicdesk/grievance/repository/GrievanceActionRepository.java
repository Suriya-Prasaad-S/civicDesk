package com.civicdesk.grievance.repository;

import com.civicdesk.grievance.entity.GrievanceAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceActionRepository extends JpaRepository<GrievanceAction, Long> {
    List<GrievanceAction> findByGrievance_GrievanceIdOrderByCreatedAtAsc(Long grievanceId);
}
