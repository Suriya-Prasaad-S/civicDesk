package com.civicdesk.grievance.repository;

import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    List<Grievance> findByUserId(Long userId);

    List<Grievance> findByStatus(GrievanceStatus status);

    List<Grievance> findByDepartmentId(Long departmentId);

    List<Grievance> findByAssignedTo(Long officerId);

    List<Grievance> findByEscalationLevel(EscalationLevel level);

    List<Grievance> findByStatusAndDepartmentId(GrievanceStatus status, Long departmentId);

    // Fetch all grievances whose SLA deadline has passed and are not yet resolved/closed/rejected
    @Query("SELECT g FROM Grievance g WHERE g.slaDeadline < :today " +
           "AND g.status NOT IN ('RESOLVED','CLOSED','REJECTED') AND g.slaBreach = false")
    List<Grievance> findSlaBreached(@Param("today") LocalDate today);

    // For compliance: count by status
    @Query("SELECT g.status, COUNT(g) FROM Grievance g GROUP BY g.status")
    List<Object[]> countByStatus();
}
