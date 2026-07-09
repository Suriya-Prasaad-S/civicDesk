package com.civicdesk.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrievanceAnalyticsResponse {
    private Long totalGrievances;
    private List<LabelCountDto> statusBreakdown;
    private List<LabelCountDto> categoryBreakdown;
    private List<LabelCountDto> assignmentBreakdown;
    private List<LabelCountDto> escalationBreakdown;
    private List<TrendDto> trend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LabelCountDto {
        private String label;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendDto {
        private String date;
        private Integer count;
    }
}
