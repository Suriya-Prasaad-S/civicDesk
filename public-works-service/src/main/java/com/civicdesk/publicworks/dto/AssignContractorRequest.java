package com.civicdesk.publicworks.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignContractorRequest {
    @NotNull(message = "contractorId is required")
    private Long contractorId;
    private String remarks;
}
