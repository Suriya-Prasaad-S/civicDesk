package com.civicdesk.analytics.repository;

import com.civicdesk.analytics.entity.CivicReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CivicReportRepository extends JpaRepository<CivicReport, String> {

    @Query("SELECT r FROM CivicReport r WHERE r.createdBy = :userId ORDER BY r.generatedDate DESC")
    Page<CivicReport> findByCreatedBy(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT r FROM CivicReport r WHERE r.createdBy = :userId AND r.status = :status ORDER BY r.generatedDate DESC")
    List<CivicReport> findByCreatedByAndStatus(@Param("userId") String userId, @Param("status") String status);

    @Query("SELECT r FROM CivicReport r WHERE r.createdBy = :userId AND r.status <> :status ORDER BY r.generatedDate DESC")
    List<CivicReport> findByCreatedByAndStatusNot(@Param("userId") String userId, @Param("status") String status);

    @Query("SELECT r FROM CivicReport r WHERE r.createdBy = :userId ORDER BY r.generatedDate DESC")
    List<CivicReport> findAllByCreatedBy(@Param("userId") String userId);

    @Query("SELECT r FROM CivicReport r WHERE r.reportId = :reportId")
    Optional<CivicReport> findByIdIncludeDeleted(@Param("reportId") String reportId);
}
