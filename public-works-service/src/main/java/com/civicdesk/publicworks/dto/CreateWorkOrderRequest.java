package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateWorkOrderRequest {

    private String projectName;

    private String category;

    private String ward;

    private String zone;

    private BigDecimal budgetAllocated;

    private LocalDate startDate;

    private LocalDate expectedEndDate;

    private String remarks;
}