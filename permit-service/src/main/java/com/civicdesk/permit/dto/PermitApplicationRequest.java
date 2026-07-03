package com.civicdesk.permit.dto;

import com.civicdesk.permit.enums.PermitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PermitApplicationRequest {

    private Long citizenId;

    @NotNull(message = "Permit type is required")
    private PermitType permitType;

    @NotBlank(message = "Property address is required")
    private String propertyAddress;

    @NotNull(message = "Validity period (in months) is required")
    @Min(value = 1, message = "Validity period must be at least 1 month")
    private Integer validityPeriod;

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.00", message = "Fee cannot be negative")
    private BigDecimal fee;

    private Long departmentId;
}
