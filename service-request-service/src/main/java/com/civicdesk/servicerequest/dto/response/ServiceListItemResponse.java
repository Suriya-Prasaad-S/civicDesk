package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * List item response for service catalog
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceListItemResponse {
    private Long serviceId;
    private String serviceName;
    private ServiceCategory category;
    private String description;
    private ServiceStatus status;
    private String departmentId;
}
