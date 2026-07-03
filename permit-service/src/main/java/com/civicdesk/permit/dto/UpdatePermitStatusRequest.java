package com.civicdesk.permit.dto;

import com.civicdesk.permit.enums.PermitStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePermitStatusRequest {
    @NotNull(message = "Status is required")
    private PermitStatus status;
    private String remarks;
}
