package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderResponse {

    private Long workOrderId;
    private String projectName;
    private String category;
    private String ward;
    private String zone;

    private BigDecimal budgetAllocated;
    private BigDecimal budgetConsumedTotal;

    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;

    private Long assignedContractorId;
    private Long assignedEngineerId;

    private String status;
    private String remarks;
}