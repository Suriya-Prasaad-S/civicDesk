package com.civicdesk.permit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private String documentId;
    private String documentType;
    private String filePath;
    private String verificationStatus;
    private String verificationRemarks;
    private LocalDateTime uploadedAt;
}
