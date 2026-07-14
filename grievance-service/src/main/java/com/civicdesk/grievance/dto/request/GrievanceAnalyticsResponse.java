package com.civicdesk.grievance.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceAnalyticsResponse {

    private Long totalGrievances;

    private List<AnalyticsCountDto> statusBreakdown;

    private List<AnalyticsCountDto> categoryBreakdown;

    private List<AnalyticsCountDto> escalationBreakdown;

    private List<AnalyticsCountDto> assignmentBreakdown;

    private List<AnalyticsTrendDto> trend;
}