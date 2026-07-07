package com.civicdesk.publicworks.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBudgetRequest {
    @NotNull(message = "budgetSpent is required")
    @PositiveOrZero(message = "budgetSpent cannot be negative")
    private BigDecimal budgetSpent;
    private String remarks;
}
