package com.civicdesk.permit.dto;

import lombok.Data;

@Data
public class VerifyDocumentRequest {
    private String verificationStatus;
    private String verificationRemarks;
}
