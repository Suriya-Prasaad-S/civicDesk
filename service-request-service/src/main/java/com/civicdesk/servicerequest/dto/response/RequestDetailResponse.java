package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.RequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed response for a single service request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestDetailResponse {
    private Long requestId;
    private Long citizenId;
    private Long userId;
    private Long serviceId;
    private String serviceName;
    private String serviceCategory;
    private String departmentId;
    private LocalDate submissionDate;
    private Long assignedOfficerId;
    private BigDecimal fee;
    private LocalDate expectedCompletionDate;
    private RequestStatus status;
    private String remarks;
    private LocalDateTime createdAt;
    private List<DocumentItemResponse> documents;
}
