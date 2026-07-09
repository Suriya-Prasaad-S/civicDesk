 package com.civicdesk.publicworks.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsLabelCountDto {

    private String label;

    private Long count;
}