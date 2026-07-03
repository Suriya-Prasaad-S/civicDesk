package com.civicdesk.permit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenewPermitRequest {
    @NotNull(message = "New validity period (in months) is required")
    @Min(value = 1, message = "Validity period must be at least 1 month")
    private Integer validityPeriod;
}
