package com.civicdesk.analytics.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrievanceAnalyticsRequest {
    private String deptId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
