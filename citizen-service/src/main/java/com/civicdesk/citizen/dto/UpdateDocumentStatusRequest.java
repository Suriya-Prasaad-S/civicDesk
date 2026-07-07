package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDocumentStatusRequest {
    @NotNull(message = "Status is required")
    private DocumentStatus status;
}
