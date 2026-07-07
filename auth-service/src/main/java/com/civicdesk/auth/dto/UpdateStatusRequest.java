package com.civicdesk.auth.dto;

import com.civicdesk.auth.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
}
