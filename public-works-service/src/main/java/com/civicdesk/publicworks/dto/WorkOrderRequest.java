package com.civicdesk.publicworks.dto;

import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WorkOrderRequest {

    @NotBlank(message = "projectName is required")
    private String projectName;

    @NotNull(message = "category is required")
    private WorkCategory category;

    private Long departmentId;
    private String ward;
    private String zone;
    private String location;
    private WorkPriority priority;

    @NotNull(message = "budgetAllocated is required")
    @Positive(message = "budgetAllocated must be positive")
    private BigDecimal budgetAllocated;

    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String remarks;
}
