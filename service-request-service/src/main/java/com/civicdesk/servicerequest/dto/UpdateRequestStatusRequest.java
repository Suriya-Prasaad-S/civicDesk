package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRequestStatusRequest {
    @NotNull(message = "Status is required")
    private RequestStatus status;
    private String remarks;
}
