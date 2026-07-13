package com.civicdesk.analytics.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PermitAnalyticsRequest {

    private LocalDate fromDate;

    private LocalDate toDate;
}