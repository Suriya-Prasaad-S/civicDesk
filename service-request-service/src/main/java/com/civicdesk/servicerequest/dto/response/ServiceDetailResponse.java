package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Detailed response for a service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceDetailResponse {
    private Long serviceId;
    private String serviceName;
    private ServiceCategory category;
    private String description;
    private String departmentId;
    private BigDecimal estimatedFee;
    private Integer estimatedDays;
    private ServiceStatus status;
    private String requiredDocuments;
    private String eligibility;
    private String applicationProcess;
    private String contactInfo;
}
