package com.civicdesk.publicworks.dto;

import com.civicdesk.publicworks.enums.MilestoneStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class MilestoneResponse {
    private Long milestoneId;
    private Long workOrderId;
    private String description;
    private LocalDate plannedDate;
    private LocalDate completedDate;
    private MilestoneStatus status;
    private Integer completionPercentage;
    private BigDecimal budgetConsumed;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
