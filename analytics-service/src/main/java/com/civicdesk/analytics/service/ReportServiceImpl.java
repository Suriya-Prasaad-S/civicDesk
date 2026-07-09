package com.civicdesk.analytics.service;

import com.civicdesk.analytics.exception.ResourceNotFoundException;
import com.civicdesk.analytics.exception.InvalidReportTypeException;
import com.civicdesk.analytics.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.analytics.dto.response.GrievanceAnalyticsResponse;
import com.civicdesk.analytics.client.GrievanceClient;
import com.civicdesk.analytics.dto.request.GenerateReportRequest;
import com.civicdesk.analytics.dto.response.ReportResponse;
import com.civicdesk.analytics.dto.response.ReportSummaryResponse;
import com.civicdesk.analytics.entity.CivicReport;
import com.civicdesk.analytics.repository.CivicReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private final CivicReportRepository repository;
    private final ObjectMapper objectMapper;
    private final GrievanceClient grievanceClient;
    private final ReportExportService reportExportService;

    @Override
    @Transactional
    public ReportResponse generateReport(GenerateReportRequest request, String userId) throws Exception {
        if (request == null) {
            throw new InvalidReportTypeException("GenerateReportRequest must not be null");
        }
        String reportType = request.getType();
        if (reportType == null || reportType.isBlank()) {
            throw new InvalidReportTypeException("Report type is required");
        }

        Map<String, Object> metrics = switch (reportType.strip().toUpperCase(Locale.ROOT)) {
            case "GRIEVANCE" -> getGrievanceAnalyticsMetrics(request);
            case "PERMIT", "SERVICE_REQUEST", "WORK_ORDER" -> {
                // TODO: implement analytics calls for these report types when their services are available.
                throw new InvalidReportTypeException("Report type not supported yet: " + reportType);
            }
            default -> throw new InvalidReportTypeException("Unsupported report type: " + reportType);
        };

        CivicReport report = new CivicReport();
        report.setReportType(reportType);
        report.setDepartmentId(request.getDepartmentId());
        report.setFromDate(request.getFromDate());
        report.setToDate(request.getToDate());
        report.setMetrics(metrics);
        report.setGeneratedDate(LocalDateTime.now());
        report.setCreatedBy(userId);
        report.setStatus("GENERATED");

        CivicReport saved = repository.save(report);

        return ReportResponse.builder()
                .reportId(saved.getReportId())
                .reportType(saved.getReportType())
                .generatedDate(saved.getGeneratedDate())
                .status(saved.getStatus())
                .build();
    }

    private Map<String, Object> getGrievanceAnalyticsMetrics(GenerateReportRequest request) {
        GrievanceAnalyticsRequest analyticsRequest = new GrievanceAnalyticsRequest();
        analyticsRequest.setFromDate(request.getFromDate());
        analyticsRequest.setToDate(request.getToDate());
        analyticsRequest.setDeptId(request.getDepartmentId());

        GrievanceAnalyticsResponse analyticsResponse = grievanceClient.getGrievanceAnalytics(analyticsRequest);
        return objectMapper.convertValue(analyticsResponse, new TypeReference<Map<String, Object>>() {
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse getReportsByUserId(String userId) {
        List<ReportResponse> reports = repository.findByCreatedByAndStatusNot(userId, "DELETED")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ReportSummaryResponse.builder()
                .reports(reports)
                .count(reports.size())
                .build();
    }

    private ReportResponse toResponse(CivicReport r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .reportType(r.getReportType())
                .status(r.getStatus())
                .generatedDate(r.getGeneratedDate())
                .build();
    }

    @Override
    @Transactional
    public byte[] downloadReport(String reportId) throws Exception {
        CivicReport report = repository.findByIdIncludeDeleted(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        // TODO: I need to convert the Status as downladed
        report.setStatus("DOWNLOADED");
        repository.save(report);
        return reportExportService.exportReportToExcel(report);
    }

    @Override
    @Transactional
    public boolean softDeleteReport(String reportId) {
        Optional<CivicReport> opt = repository.findByIdIncludeDeleted(reportId);
        if (opt.isEmpty()) {
            return false;
        }

        CivicReport report = opt.get();
        report.setStatus("DELETED");
        repository.save(report);
        return true;
    }
}
