package com.civicdesk.grievance.dto;

import com.civicdesk.grievance.enums.EscalationLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EscalateRequest {
    @NotNull(message = "escalationLevel is required")
    private EscalationLevel escalationLevel;
    private String remarks;
}
