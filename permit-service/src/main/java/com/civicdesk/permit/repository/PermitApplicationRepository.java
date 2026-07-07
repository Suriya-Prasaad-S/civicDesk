package com.civicdesk.permit.repository;

import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import com.civicdesk.permit.dto.AnalyticsLabelCountResponse;
import com.civicdesk.permit.dto.AnalyticsTrendResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
@Repository
public interface PermitApplicationRepository extends JpaRepository<PermitApplication, Long> {
    List<PermitApplication> findByUserId(Long userId);
    List<PermitApplication> findByCitizenId(Long citizenId);
    List<PermitApplication> findByStatus(PermitStatus status);
    List<PermitApplication> findByPermitType(PermitType permitType);
    List<PermitApplication> findByDepartmentId(Long departmentId);
    List<PermitApplication> findByStatusAndDepartmentId(PermitStatus status, Long departmentId);
    // Used by compliance to find expiring permits
    List<PermitApplication> findByStatusAndExpiryDateBefore(PermitStatus status, LocalDate date);

    @Query("""
SELECT COUNT(p)
FROM PermitApplication p
WHERE p.applicationDate >= :from
AND p.applicationDate <= :to
""")
    long countPermits(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
SELECT p.status as label,
       COUNT(p) as count
FROM PermitApplication p
WHERE p.applicationDate >= :from
AND p.applicationDate <= :to
GROUP BY p.status
""")
    <T> List<T> getStatusBreakdown(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type);

    @Query("""
SELECT p.permitType as label,
       COUNT(p) as count
FROM PermitApplication p
WHERE p.applicationDate >= :from
AND p.applicationDate <= :to
GROUP BY p.permitType
""")
    <T> List<T> getPermitTypeBreakdown(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type);

    @Query("""
SELECT p.applicationDate as date,
       COUNT(p) as count
FROM PermitApplication p
WHERE p.applicationDate >= :from
AND p.applicationDate <= :to
GROUP BY p.applicationDate
ORDER BY p.applicationDate
""")
    <T> List<T> getApplicationTrend(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Class<T> type);

    @Query("""
SELECT p.decisionDate as date,
       COUNT(p) as count
FROM PermitApplication p
WHERE p.decisionDate IS NOT NULL
GROUP BY p.decisionDate
ORDER BY p.decisionDate
""")
    <T> List<T> getDecisionTrend(
            Class<T> type);

    @Query("""
SELECT p
FROM PermitApplication p
WHERE p.decisionDate IS NOT NULL
""")
    List<PermitApplication> getDecidedPermits();




}
