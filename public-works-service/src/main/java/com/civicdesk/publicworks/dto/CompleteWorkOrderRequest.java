package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompleteWorkOrderRequest {

    private LocalDate actualEndDate;

    private String remarks;
}