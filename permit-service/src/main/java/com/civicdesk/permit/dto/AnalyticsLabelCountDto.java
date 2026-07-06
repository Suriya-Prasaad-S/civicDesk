package com.civicdesk.permit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsLabelCountDto {

    private String label;

    private Long count;
}