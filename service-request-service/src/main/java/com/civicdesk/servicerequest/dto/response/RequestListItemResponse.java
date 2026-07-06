package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.RequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * List item response for service requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestListItemResponse {
    private Long requestId;
    private Long citizenId;
    private Long serviceId;
    private String serviceName;
    private LocalDate submissionDate;
    private RequestStatus status;
    private Long assignedOfficerId;
}
