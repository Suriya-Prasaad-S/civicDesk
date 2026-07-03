package com.civicdesk.publicworks.dto;

import com.civicdesk.publicworks.enums.MilestoneStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MilestoneUpdateRequest {
    @NotNull(message = "status is required")
    private MilestoneStatus status;

    @Min(0) @Max(100)
    private Integer completionPercentage;

    private LocalDate completedDate;
    private BigDecimal budgetConsumed;
    private String remarks;
}
