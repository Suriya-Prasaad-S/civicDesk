package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.ServiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateServiceStatusRequest {
    @NotNull(message = "Status is required")
    private ServiceStatus status;
}
