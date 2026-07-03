package com.civicdesk.analytics.dto;

import com.civicdesk.analytics.enums.ReportStatus;
import com.civicdesk.analytics.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class CivicReportResponse {
    private Long reportId;
    private String reportName;
    private ReportType reportType;
    private Long generatedBy;
    private ReportStatus status;
    private String parameters;
    private String reportData;
    private LocalDateTime generatedAt;
}
