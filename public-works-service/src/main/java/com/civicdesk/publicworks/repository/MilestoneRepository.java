package com.civicdesk.publicworks.repository;

import com.civicdesk.publicworks.entity.Milestone;
import com.civicdesk.publicworks.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByWorkOrder_WorkOrderId(Long workOrderId);

    List<Milestone> findByStatus(MilestoneStatus status);

    List<Milestone> findByStatusAndPlannedDateBefore(MilestoneStatus status, LocalDate date);
}
