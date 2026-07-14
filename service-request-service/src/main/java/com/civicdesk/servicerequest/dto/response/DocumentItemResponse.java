package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document item for request details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentItemResponse {
    private Long documentId;
    private Long requestId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String downloadUrl;
    private VerificationStatus verificationStatus;
    private String verificationRemarks;
}
