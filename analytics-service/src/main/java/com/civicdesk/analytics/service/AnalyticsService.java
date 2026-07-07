package com.civicdesk.analytics.service;

import com.civicdesk.analytics.dto.CivicReportResponse;
import com.civicdesk.analytics.dto.DashboardMetrics;
import com.civicdesk.analytics.dto.GenerateReportRequest;
import com.civicdesk.analytics.entity.CivicReport;
import com.civicdesk.analytics.enums.ReportStatus;
import com.civicdesk.analytics.enums.ReportType;
import com.civicdesk.analytics.exception.ResourceNotFoundException;
import com.civicdesk.analytics.repository.CivicReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final CivicReportRepository reportRepository;

    // ─── DASHBOARD ────────────────────────────────────────────────────────────

    /**
     * Returns aggregate counts from the civic_reports snapshots stored in this service's DB.
     * In a production setup, these numbers would come from an event-driven data pipeline
     * (e.g., Kafka consumers aggregating events from all services). For this phase, the
     * dashboard reads the latest stored report snapshots.
     */
    public DashboardMetrics getDashboard() {
        long totalReports = reportRepository.count();

        // Count reports by type as a proxy for activity tracking
        Map<String, Long> byType = new java.util.HashMap<>();
        for (ReportType type : ReportType.values()) {
            byType.put(type.name(), (long) reportRepository.findByReportTypeOrderByGeneratedAtDesc(type).size());
        }

        return DashboardMetrics.builder()
                // These values represent report-based metrics in the analytics DB.
                // In full integration they come from event streams; placeholders shown here
                // correctly reflect what this service owns.
                .totalGrievances(0L)
                .openGrievances(0L)
                .resolvedGrievances(0L)
                .slaBreachedGrievances(0L)
                .escalatedGrievances(0L)
                .totalPermits(0L)
                .pendingPermits(0L)
                .approvedPermits(0L)
                .rejectedPermits(0L)
                .totalServiceRequests(0L)
                .pendingServiceRequests(0L)
                .completedServiceRequests(0L)
                .totalWorkOrders(0L)
                .activeWorkOrders(0L)
                .completedWorkOrders(0L)
                .breakdownByCategory(byType)
                .breakdownByStatus(Map.of("totalStoredReports", totalReports))
                .build();
    }

    // ─── REPORT GENERATION ────────────────────────────────────────────────────

    @Transactional
    public CivicReportResponse generateReport(GenerateReportRequest request, Long userId) {
        String parameters = buildParametersJson(request);

        // In full integration each report type would query relevant service DBs via REST/events.
        // Here we store the report skeleton with parameters — extensible for data pipeline attachment.
        String reportData = buildReportDataSkeleton(request.getReportType());

        CivicReport report = CivicReport.builder()
                .reportName(request.getReportName())
                .reportType(request.getReportType())
                .generatedBy(userId)
                .status(ReportStatus.GENERATED)
                .parameters(parameters)
                .reportData(reportData)
                .build();

        CivicReport saved = reportRepository.save(report);
        log.info("Report generated: id={} type={} by={}", saved.getReportId(), saved.getReportType(), userId);
        return mapToResponse(saved);
    }

    public List<CivicReportResponse> getAll() {
        return reportRepository.findAllByOrderByGeneratedAtDesc()
                .stream().map(this::mapToResponse).toList();
    }

    public List<CivicReportResponse> getByType(ReportType type) {
        return reportRepository.findByReportTypeOrderByGeneratedAtDesc(type)
                .stream().map(this::mapToResponse).toList();
    }

    public List<CivicReportResponse> getMyReports(Long userId) {
        return reportRepository.findByGeneratedByOrderByGeneratedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    public CivicReportResponse getById(Long reportId) {
        return mapToResponse(reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId)));
    }

    @Transactional
    public void deleteReport(Long reportId) {
        CivicReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        reportRepository.delete(report);
        log.info("Report deleted: id={}", reportId);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String buildParametersJson(GenerateReportRequest request) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"reportType\":\"").append(request.getReportType()).append("\"");
        if (request.getDepartmentId() != null)
            sb.append(",\"departmentId\":").append(request.getDepartmentId());
        if (request.getFromDate() != null)
            sb.append(",\"fromDate\":\"").append(request.getFromDate()).append("\"");
        if (request.getToDate() != null)
            sb.append(",\"toDate\":\"").append(request.getToDate()).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String buildReportDataSkeleton(ReportType type) {
        return switch (type) {
            case GRIEVANCE_SUMMARY -> "{\"description\":\"Grievance summary report\",\"metrics\":{\"total\":0,\"open\":0,\"resolved\":0,\"slaBreached\":0,\"escalated\":0}}";
            case PERMIT_SUMMARY    -> "{\"description\":\"Permit summary report\",\"metrics\":{\"total\":0,\"applied\":0,\"approved\":0,\"rejected\":0,\"expired\":0}}";
            case SERVICE_REQUEST_SUMMARY -> "{\"description\":\"Service request summary\",\"metrics\":{\"total\":0,\"submitted\":0,\"approved\":0,\"completed\":0,\"rejected\":0}}";
            case WORK_ORDER_SUMMARY -> "{\"description\":\"Work order summary\",\"metrics\":{\"total\":0,\"planned\":0,\"inProgress\":0,\"completed\":0,\"cancelled\":0}}";
            case SLA_COMPLIANCE    -> "{\"description\":\"SLA compliance report\",\"metrics\":{\"totalGrievances\":0,\"withinSla\":0,\"breached\":0,\"complianceRate\":\"0%\"}}";
            case BUDGET_UTILISATION -> "{\"description\":\"Budget utilisation report\",\"metrics\":{\"totalAllocated\":0,\"totalSpent\":0,\"totalRemaining\":0,\"overrunCount\":0}}";
            case USER_ACTIVITY      -> "{\"description\":\"User activity report\",\"metrics\":{\"totalLogins\":0,\"activeUsers\":0,\"newRegistrations\":0}}";
            case DEPARTMENT_PERFORMANCE -> "{\"description\":\"Department performance report\",\"metrics\":{\"avgResolutionDays\":0,\"slaBreachRate\":\"0%\",\"grievancesHandled\":0}}";
        };
    }

    private CivicReportResponse mapToResponse(CivicReport r) {
        return CivicReportResponse.builder()
                .reportId(r.getReportId())
                .reportName(r.getReportName())
                .reportType(r.getReportType())
                .generatedBy(r.getGeneratedBy())
                .status(r.getStatus())
                .parameters(r.getParameters())
                .reportData(r.getReportData())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}
