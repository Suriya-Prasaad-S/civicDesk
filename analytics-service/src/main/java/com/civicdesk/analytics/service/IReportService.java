package com.civicdesk.analytics.service;

import com.civicdesk.analytics.dto.request.GenerateReportRequest;
import com.civicdesk.analytics.dto.response.ReportResponse;
import com.civicdesk.analytics.dto.response.ReportSummaryResponse;

public interface IReportService {
    ReportResponse generateReport(GenerateReportRequest request, String userId) throws Exception;
    byte[] downloadReport(String reportId) throws Exception;
    boolean softDeleteReport(String reportId);
    ReportSummaryResponse getReportsByUserId(String userId);
}
