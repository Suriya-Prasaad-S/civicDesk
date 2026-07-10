package com.civicdesk.analytics.service;

import com.civicdesk.analytics.exception.InvalidReportTypeException;
import com.civicdesk.analytics.exception.ResourceNotFoundException;
import com.civicdesk.analytics.client.GrievanceFeignClient;
import com.civicdesk.analytics.client.PermitFeignClient;
import com.civicdesk.analytics.client.ServiceRequestFeignClient;
import com.civicdesk.analytics.client.WorkOrderFeignClient;
import com.civicdesk.analytics.dto.request.GenerateReportRequest;
import com.civicdesk.analytics.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.analytics.dto.request.PermitAnalyticsRequest;
import com.civicdesk.analytics.dto.request.ServiceRequestAnalyticsRequest;
import com.civicdesk.analytics.dto.request.WorkOrderAnalyticsRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;
import com.civicdesk.analytics.dto.response.GrievanceAnalyticsResponse;
import com.civicdesk.analytics.dto.response.PermitAnalyticsResponse;
import com.civicdesk.analytics.dto.response.ReportResponse;
import com.civicdesk.analytics.dto.response.ReportSummaryResponse;
import com.civicdesk.analytics.dto.response.ServiceRequestAnalyticsResponse;
import com.civicdesk.analytics.dto.response.WorkOrderAnalyticsResponse;
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
    private final ReportExportService reportExportService;
    private final GrievanceFeignClient grievanceFeignClient;
    private final PermitFeignClient permitFeignClient;
    private final ServiceRequestFeignClient serviceRequestFeignClient;
    private final WorkOrderFeignClient workOrderFeignClient;

        @Override
        @Transactional
        public ReportResponse generateReport(
                GenerateReportRequest request,
                String userId) throws Exception {

                validateGenerateReportRequest(request);

                String reportType = request.getType().strip().toUpperCase(Locale.ROOT);

                Map<String, Object> metrics = switch (reportType) {
                case "GRIEVANCE" -> getGrievanceAnalyticsMetrics(request);
                case "PERMIT" -> getPermitAnalyticsMetrics(request);
                case "SERVICE_REQUEST" -> getServiceRequestAnalyticsMetrics(request);
                case "WORK_ORDER" -> getWorkOrderAnalyticsMetrics(request);
                default -> throw new InvalidReportTypeException(
                        "Unsupported report type: " + reportType);
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

        private void validateGenerateReportRequest(
                GenerateReportRequest request) {

                if (request == null) {
                throw new InvalidReportTypeException(
                        "GenerateReportRequest must not be null");
                }

                if (request.getType() == null ||
                        request.getType().isBlank()) {
                throw new InvalidReportTypeException(
                        "Report type is required");
                }
        }    

        private Map<String, Object> getGrievanceAnalyticsMetrics(
                GenerateReportRequest request) {

                GrievanceAnalyticsRequest analyticsRequest =
                        new GrievanceAnalyticsRequest();

                analyticsRequest.setFromDate(request.getFromDate());
                analyticsRequest.setToDate(request.getToDate());
                analyticsRequest.setDeptId(request.getDepartmentId());

                ApiResponse response =
                        grievanceFeignClient.getGrievanceAnalytics(
                                analyticsRequest);

                GrievanceAnalyticsResponse analyticsResponse =
                        objectMapper.convertValue(
                                response.getData(),
                                GrievanceAnalyticsResponse.class);

                return objectMapper.convertValue(
                        analyticsResponse,
                        new TypeReference<Map<String, Object>>() {});
        }

        private Map<String, Object> getPermitAnalyticsMetrics(
                GenerateReportRequest request) {

                PermitAnalyticsRequest permitRequest =
                        new PermitAnalyticsRequest();

                permitRequest.setFromDate(
                        request.getFromDate().toLocalDate());

                permitRequest.setToDate(
                        request.getToDate().toLocalDate());

                ApiResponse response =
                        permitFeignClient.getPermitAnalytics(
                                permitRequest);

                PermitAnalyticsResponse analyticsResponse =
                        objectMapper.convertValue(
                                response.getData(),
                                PermitAnalyticsResponse.class);

                return objectMapper.convertValue(
                        analyticsResponse,
                        new TypeReference<Map<String, Object>>() {
                        });
        }

        private Map<String, Object> getServiceRequestAnalyticsMetrics(
                GenerateReportRequest request) {

                ServiceRequestAnalyticsRequest analyticsRequest =
                        new ServiceRequestAnalyticsRequest(
                                request.getDepartmentId() != null
                                        ? Long.valueOf(request.getDepartmentId())
                                        : null,
                                request.getFromDate().toLocalDate(),
                                request.getToDate().toLocalDate()
                        );

                ServiceRequestAnalyticsResponse analyticsResponse =
                        serviceRequestFeignClient.getServiceRequestAnalytics(
                                analyticsRequest
                        );

                return objectMapper.convertValue(
                        analyticsResponse,
                        new TypeReference<Map<String, Object>>() {
                        });
        }
    
        private Map<String, Object> getWorkOrderAnalyticsMetrics(
                GenerateReportRequest request) {

                WorkOrderAnalyticsRequest analyticsRequest =
                        new WorkOrderAnalyticsRequest();

                analyticsRequest.setFromDate(
                        request.getFromDate().toLocalDate());

                analyticsRequest.setToDate(
                        request.getToDate().toLocalDate());

                ApiResponse response =
                        workOrderFeignClient.getAnalytics(
                                analyticsRequest);

                WorkOrderAnalyticsResponse analyticsResponse =
                        objectMapper.convertValue(
                                response.getData(),
                                WorkOrderAnalyticsResponse.class);

                return objectMapper.convertValue(
                        analyticsResponse,
                        new TypeReference<Map<String, Object>>() {
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
