package com.civicdesk.analytics.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WorkOrderAnalyticsResponse {

    private Long totalWorkOrders;

    private Long completedWorkOrders;

    private Long delayedWorkOrders;

    private Double averageCompletionDays;

    private BudgetAnalytics budget;

    private MilestoneAnalytics milestones;

    private List<AnalyticsLabelCountDto> statusBreakdown;

    private List<AnalyticsLabelCountDto> categoryBreakdown;

    private List<AnalyticsLabelCountDto> wardBreakdown;

    @Data
    public static class BudgetAnalytics {

        private BigDecimal allocated;

        private BigDecimal consumed;

        private Double utilizationPercentage;
    }

    @Data
    public static class MilestoneAnalytics {

        private Long delayedMilestones;

        private List<AnalyticsLabelCountDto> statusBreakdown;
    }
}