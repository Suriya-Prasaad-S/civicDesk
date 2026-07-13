package com.civicdesk.grievance.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.civicdesk.grievance.dto.request.AnalyticsCountDto;
import com.civicdesk.grievance.dto.request.AnalyticsTrendDto;
import com.civicdesk.grievance.entity.Grievance;


@Repository
public interface GrievanceRepo extends JpaRepository<Grievance, String> {

    /** Grievances raised by a given citizen. */
    List<Grievance> findByCitizenId(String citizenId);

    /** Grievances assigned to a given user (e.g. a field officer). */
    List<Grievance> findByAssignedToId(String assignedToId);

    /** Grievances belonging to a department (the supervisor's queue). */
    List<Grievance> findByDepartmentId(String departmentId);


   // ✅ Total count
    @Query("""
        SELECT COUNT(g)
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
    """)
    long countGrievances(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );


    // ✅ Status Breakdown
    @Query("""
        SELECT new com.civicdesk.grievance.dto.request.AnalyticsCountDto(g.status, COUNT(g))
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
        GROUP BY g.status
    """)
    List<AnalyticsCountDto> getStatusBreakdown(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );


    // ✅ Category Breakdown
    @Query("""
        SELECT new com.civicdesk.grievance.dto.request.AnalyticsCountDto(g.category, COUNT(g))
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
        GROUP BY g.category
    """)
    List<AnalyticsCountDto> getCategoryBreakdown(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );


    // ✅ Escalation Breakdown
    @Query("""
        SELECT new com.civicdesk.grievance.dto.request.AnalyticsCountDto(g.escalationLevel, COUNT(g))
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
        GROUP BY g.escalationLevel
    """)
    List<AnalyticsCountDto> getEscalationBreakdown(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );


    // ✅ Assignment Breakdown
    @Query("""
        SELECT new com.civicdesk.grievance.dto.request.AnalyticsCountDto(
          CASE 
            WHEN g.assignedToId IS NULL THEN 'UNASSIGNED'
            ELSE 'ASSIGNED'
          END,
          COUNT(g)
        )
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
        GROUP BY
          CASE 
            WHEN g.assignedToId IS NULL THEN 'UNASSIGNED'
            ELSE 'ASSIGNED'
          END
    """)
    List<AnalyticsCountDto> getAssignmentBreakdown(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );


    // ✅ Trend (date-wise)
    @Query("""
        SELECT new com.civicdesk.grievance.dto.request.AnalyticsTrendDto(DATE(g.submissionDate), COUNT(g))
        FROM Grievance g
        WHERE g.submissionDate >= :from
        AND g.submissionDate < :to
        AND (:dept IS NULL OR g.departmentId = :dept)
        GROUP BY DATE(g.submissionDate)
        ORDER BY DATE(g.submissionDate)
    """)
    List<AnalyticsTrendDto> getTrend(
        @Param("dept") String departmentId,
        @Param("from") LocalDateTime fromDate,
        @Param("to") LocalDateTime toDate
    );

}
