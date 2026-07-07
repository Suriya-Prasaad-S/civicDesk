package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.DocumentStatus;
import com.civicdesk.citizen.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CitizenDocumentResponse {
    private Long documentId;
    private Long citizenId;
    private DocumentType documentType;
    private String filePath;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
}
