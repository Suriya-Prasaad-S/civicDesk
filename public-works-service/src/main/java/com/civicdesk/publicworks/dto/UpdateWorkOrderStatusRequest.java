package com.civicdesk.publicworks.dto;

import com.civicdesk.publicworks.enums.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateWorkOrderStatusRequest {
    @NotNull(message = "status is required")
    private WorkOrderStatus status;
    private String remarks;
}
