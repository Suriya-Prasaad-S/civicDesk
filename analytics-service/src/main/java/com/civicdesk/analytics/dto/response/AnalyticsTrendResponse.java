package com.civicdesk.analytics.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AnalyticsTrendResponse {

    private LocalDate date;

    private Long count;
}