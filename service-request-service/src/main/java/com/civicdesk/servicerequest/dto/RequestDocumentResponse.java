package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestDocumentResponse {
    private String docId;
    private String documentType;
    private VerificationStatus verificationStatus;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate uploadedOn;
    private String message;
}
