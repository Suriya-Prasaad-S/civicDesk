package com.civicdesk.analytics.repository;

import com.civicdesk.analytics.entity.CivicReport;
import com.civicdesk.analytics.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CivicReportRepository extends JpaRepository<CivicReport, Long> {
    List<CivicReport> findByReportTypeOrderByGeneratedAtDesc(ReportType type);
    List<CivicReport> findByGeneratedByOrderByGeneratedAtDesc(Long userId);
    List<CivicReport> findAllByOrderByGeneratedAtDesc();
}
