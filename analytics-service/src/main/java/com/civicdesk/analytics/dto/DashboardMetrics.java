package com.civicdesk.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data @Builder
public class DashboardMetrics {
    // Grievance stats
    private long totalGrievances;
    private long openGrievances;
    private long resolvedGrievances;
    private long slaBreachedGrievances;
    private long escalatedGrievances;

    // Permit stats
    private long totalPermits;
    private long pendingPermits;
    private long approvedPermits;
    private long rejectedPermits;

    // Service request stats
    private long totalServiceRequests;
    private long pendingServiceRequests;
    private long completedServiceRequests;

    // Work order stats
    private long totalWorkOrders;
    private long activeWorkOrders;
    private long completedWorkOrders;

    // Breakdown by category / department (optional, populated per report type)
    private Map<String, Long> breakdownByCategory;
    private Map<String, Long> breakdownByDepartment;
    private Map<String, Long> breakdownByStatus;
}
