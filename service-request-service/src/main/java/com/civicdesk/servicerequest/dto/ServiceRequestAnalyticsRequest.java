package com.civicdesk.servicerequest.dto;

import java.time.LocalDate;

public record ServiceRequestAnalyticsRequest(
        Long deptId,
        LocalDate fromDate,
        LocalDate toDate
) {
}
