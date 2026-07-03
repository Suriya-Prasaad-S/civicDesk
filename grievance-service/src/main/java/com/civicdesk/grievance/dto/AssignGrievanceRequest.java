package com.civicdesk.grievance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignGrievanceRequest {
    @NotNull(message = "officerId is required")
    private Long officerId;
    private String remarks;
}
