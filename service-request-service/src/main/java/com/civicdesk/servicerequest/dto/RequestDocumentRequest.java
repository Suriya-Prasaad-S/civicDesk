package com.civicdesk.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestDocumentRequest {
    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "File path is required")
    private String filePath;
}
