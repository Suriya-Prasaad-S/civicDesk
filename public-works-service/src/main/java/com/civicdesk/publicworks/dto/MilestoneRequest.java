package com.civicdesk.publicworks.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MilestoneRequest {

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "plannedDate is required")
    private LocalDate plannedDate;

    @Min(0) @Max(100)
    private Integer completionPercentage;

    private BigDecimal budgetConsumed;

    private String remarks;
}
