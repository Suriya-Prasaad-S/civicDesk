package com.civicdesk.servicerequest.dto;

import java.time.LocalDate;
import java.util.List;

public record ServiceRequestAnalyticsResponse(
        Long totalRequests,
        List<LabelCount> statusBreakdown,
        List<LabelCount> serviceBreakdown,
        List<DateCount> trend,
        Long overdueRequests
) {
    public record LabelCount(String label, Long count) {
    }

    public record DateCount(LocalDate date, Long count) {
    }
}
