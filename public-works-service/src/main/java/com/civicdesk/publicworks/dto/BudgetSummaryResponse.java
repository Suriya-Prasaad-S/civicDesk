package com.civicdesk.publicworks.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetSummaryResponse {

    private String status;

    private Long workOrderCount;

    private BigDecimal totalAllocated;

    private BigDecimal totalSpent;

    private BigDecimal totalRemaining;
}