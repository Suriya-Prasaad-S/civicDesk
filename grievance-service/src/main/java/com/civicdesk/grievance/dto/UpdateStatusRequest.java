package com.civicdesk.grievance.dto;

import com.civicdesk.grievance.enums.GrievanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "status is required")
    private GrievanceStatus status;
    private String remarks;
}
