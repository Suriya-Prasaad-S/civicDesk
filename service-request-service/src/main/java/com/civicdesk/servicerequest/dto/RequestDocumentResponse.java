package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequestDocumentResponse {
    private Long docSubmissionId;
    private Long requestId;
    private String documentType;
    private String filePath;
    private LocalDateTime uploadedDate;
    private VerificationStatus verificationStatus;
}
