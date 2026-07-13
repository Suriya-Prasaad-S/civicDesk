package com.civicdesk.analytics.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PermitAnalyticsResponse {

    private long totalPermits;

    private List<AnalyticsLabelCountDto> statusBreakdown;

    private List<AnalyticsLabelCountDto> permitTypeBreakdown;

    private List<AnalyticsTrendResponse> applicationTrend;

    private List<AnalyticsTrendResponse> decisionTrend;

    private Double averageDecisionDays;

    private InspectionAnalytics inspection;

    @Data
    public static class InspectionAnalytics {

        private List<AnalyticsLabelCountDto> statusBreakdown;

        private List<AnalyticsLabelCountDto> outcomeBreakdown;
    }
}