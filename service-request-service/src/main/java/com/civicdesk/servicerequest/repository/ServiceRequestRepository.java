package com.civicdesk.servicerequest.repository;

import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserId(Long userId);
    List<ServiceRequest> findByCitizenId(Long citizenId);
    List<ServiceRequest> findByAssignedOfficerId(Long officerId);
    List<ServiceRequest> findByStatus(RequestStatus status);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.service.departmentId = :deptId")
    List<ServiceRequest> findByDepartmentId(@Param("deptId") Long departmentId);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.service.departmentId = :deptId AND sr.status = :status")
    List<ServiceRequest> findByDepartmentIdAndStatus(@Param("deptId") Long departmentId,
                                                      @Param("status") RequestStatus status);

    List<ServiceRequest> findByAssignedOfficerIdAndStatus(Long officerId, RequestStatus status);

    @Query("""
            SELECT COUNT(sr)
            FROM ServiceRequest sr
            WHERE (:deptId IS NULL OR sr.service.departmentId = :deptId)
              AND (:fromDate IS NULL OR sr.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR sr.submissionDate <= :toDate)
            """)
    long countRequests(
            @Param("deptId") Long deptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT sr.status, COUNT(sr)
            FROM ServiceRequest sr
            WHERE (:deptId IS NULL OR sr.service.departmentId = :deptId)
              AND (:fromDate IS NULL OR sr.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR sr.submissionDate <= :toDate)
            GROUP BY sr.status
            ORDER BY COUNT(sr) DESC
            """)
    List<Object[]> getStatusBreakdown(
            @Param("deptId") Long deptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT sr.service.serviceName, COUNT(sr)
            FROM ServiceRequest sr
            WHERE (:deptId IS NULL OR sr.service.departmentId = :deptId)
              AND (:fromDate IS NULL OR sr.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR sr.submissionDate <= :toDate)
            GROUP BY sr.service.serviceName
            ORDER BY COUNT(sr) DESC
            """)
    List<Object[]> getServiceBreakdown(
            @Param("deptId") Long deptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT sr.submissionDate, COUNT(sr)
            FROM ServiceRequest sr
            WHERE (:deptId IS NULL OR sr.service.departmentId = :deptId)
              AND (:fromDate IS NULL OR sr.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR sr.submissionDate <= :toDate)
            GROUP BY sr.submissionDate
            ORDER BY sr.submissionDate
            """)
    List<Object[]> getTrend(
            @Param("deptId") Long deptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(sr)
            FROM ServiceRequest sr
            WHERE (:deptId IS NULL OR sr.service.departmentId = :deptId)
              AND (:fromDate IS NULL OR sr.submissionDate >= :fromDate)
              AND (:toDate IS NULL OR sr.submissionDate <= :toDate)
              AND sr.status <> com.civicdesk.servicerequest.enums.RequestStatus.COMPLETED
              AND sr.expectedCompletionDate < :today
            """)
    long countOverdueRequests(
            @Param("deptId") Long deptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("today") LocalDate today);

    @Query("""
            SELECT sr
            FROM ServiceRequest sr
            WHERE sr.status <> com.civicdesk.servicerequest.enums.RequestStatus.COMPLETED
              AND (sr.slaBreach = false OR sr.slaBreach IS NULL)
              AND sr.expectedCompletionDate < :today
            """)
    List<ServiceRequest> findOverdueRequests(@Param("today") LocalDate today);
}
