package com.civicdesk.analytics.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderAnalyticsRequest {

    private LocalDate fromDate;

    private LocalDate toDate;
}