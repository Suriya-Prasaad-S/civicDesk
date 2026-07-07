package com.civicdesk.publicworks.dto;

import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.enums.WorkPriority;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class WorkOrderResponse {
    private Long workOrderId;
    private String projectName;
    private WorkCategory category;
    private Long departmentId;
    private String ward;
    private String zone;
    private String location;
    private Long assignedContractorId;
    private Long assignedEngineerId;
    private WorkPriority priority;
    private WorkOrderStatus status;
    private BigDecimal budgetAllocated;
    private BigDecimal budgetConsumedTotal;
    private BigDecimal budgetRemaining;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
