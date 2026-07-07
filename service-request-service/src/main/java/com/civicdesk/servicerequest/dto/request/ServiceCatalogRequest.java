package com.civicdesk.servicerequest.dto.request;

import com.civicdesk.servicerequest.enums.ServiceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceCatalogRequest {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotNull(message = "Department ID is required")
    private String departmentId;

    @NotNull(message = "Category is required")
    private ServiceCategory category;

    @NotNull(message = "Processing days is required")
    @Min(value = 1, message = "Processing days must be at least 1")
    private Integer processingDays;

    private List<String> requiredDocuments;

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.00", message = "Fee cannot be negative")
    private BigDecimal fee;
}
