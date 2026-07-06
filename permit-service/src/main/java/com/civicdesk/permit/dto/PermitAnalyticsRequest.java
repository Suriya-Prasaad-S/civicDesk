package com.civicdesk.permit.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PermitAnalyticsRequest {

    private LocalDate fromDate;

    private LocalDate toDate;
}
