package com.civicdesk.analytics.controller;


import com.civicdesk.analytics.dto.request.GenerateReportRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;
import com.civicdesk.analytics.dto.response.ReportSummaryResponse;
import com.civicdesk.analytics.service.IReportService;
import com.civicdesk.analytics.enums.AuditAction;
import com.civicdesk.analytics.enums.AuditModule;
import com.civicdesk.analytics.client.AuditClient;
import com.civicdesk.analytics.util.ClientIpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/civicDesk/analytics/reports")
@RequiredArgsConstructor
@Validated
@Slf4j
@PreAuthorize("hasAnyRole('DS','ADM')")
public class ReportController {

    private final IReportService reportService;
    private final AuditClient auditClient;

    @PostMapping
    public ResponseEntity<ApiResponse> generateReport(@Valid @RequestBody GenerateReportRequest request,
                                                       HttpServletRequest httpReq) throws Exception {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Generating report for user: {}", userId);
        reportService.generateReport(request, userId);
        auditClient.logAudit(userId, AuditAction.GENERATE_REPORT.name(), AuditModule.ANALYTICS.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.status(201).body(ApiResponse.of("Report is ready", null));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getReportsByUser(@PathVariable("userId") @NotBlank String userId) {
        ReportSummaryResponse response = reportService.getReportsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.data(response));
    }


    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String id,
            HttpServletRequest httpReq) throws Exception {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] fileBytes = reportService.downloadReport(id);
        auditClient.logAudit(userId, AuditAction.DOWNLOAD_REPORT.name(), AuditModule.ANALYTICS.name(), ClientIpUtil.resolve(httpReq));

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report-" + id + ".xlsx\"")
            .contentType(
                MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(fileBytes.length)
            .body(fileBytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteReport(@PathVariable("id") @NotBlank String id,
                                                    HttpServletRequest httpReq) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean deleted = reportService.softDeleteReport(id);
        auditClient.logAudit(userId, AuditAction.DELETE_REPORT.name(), AuditModule.ANALYTICS.name(), ClientIpUtil.resolve(httpReq));
        if (!deleted) {
            return ResponseEntity.status(404).body(ApiResponse.error("Report not found"));
        }
        return ResponseEntity.ok(ApiResponse.of("Report deleted successfully", null));
    }
}
