package com.civicdesk.servicerequest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceRequestCreateRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    // @NotNull(message = "Citizen ID is required")
    // private Long citizenId;
}
