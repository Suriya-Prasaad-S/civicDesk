package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateWorkOrderRequest {

    private String projectName;

    private BigDecimal budgetAllocated;

    private LocalDate expectedEndDate;

    private String remarks;
}
