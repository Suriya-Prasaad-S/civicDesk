package com.civicdesk.publicworks.repository;

import com.civicdesk.publicworks.entity.Milestone;
import com.civicdesk.publicworks.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByWorkOrder_WorkOrderId(Long workOrderId);

    List<Milestone> findByStatus(MilestoneStatus status);

    List<Milestone> findByStatusAndPlannedDateBefore(MilestoneStatus status, LocalDate date);

    @Query("""
SELECT COUNT(m)
FROM Milestone m
WHERE m.status = 'DELAYED'
OR (
    m.status = 'PENDING'
    AND m.plannedDate < CURRENT_DATE
)
""")
    Long countDelayedMilestones();
}
