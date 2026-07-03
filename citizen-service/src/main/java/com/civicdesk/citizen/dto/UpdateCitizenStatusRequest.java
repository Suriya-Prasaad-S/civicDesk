package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.CitizenStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCitizenStatusRequest {
    @NotNull(message = "Status is required")
    private CitizenStatus status;
}
