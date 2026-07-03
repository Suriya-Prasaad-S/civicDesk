package com.civicdesk.analytics.dto;

import com.civicdesk.analytics.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateReportRequest {

    @NotBlank(message = "reportName is required")
    private String reportName;

    @NotNull(message = "reportType is required")
    private ReportType reportType;

    // Optional: department filter
    private Long departmentId;

    // Optional: date range (ISO format yyyy-MM-dd)
    private String fromDate;
    private String toDate;
}
