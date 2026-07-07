package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyDocumentRequest {
    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;
}
