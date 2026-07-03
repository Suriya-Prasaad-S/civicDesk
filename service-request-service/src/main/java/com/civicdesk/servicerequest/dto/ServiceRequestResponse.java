package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceRequestResponse {
    private Long requestId;
    private Long citizenId;
    private Long userId;
    private Long serviceId;
    private String serviceName;
    private String serviceCategory;
    private Long departmentId;
    private LocalDate submissionDate;
    private Long assignedOfficerId;
    private BigDecimal fee;
    private LocalDate expectedCompletionDate;
    private RequestStatus status;
    private String remarks;
    private LocalDateTime createdAt;
}
