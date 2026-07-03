package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CitizenDocumentRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    private String filePath;

    private LocalDate issuedDate;
    private LocalDate expiryDate;
}
