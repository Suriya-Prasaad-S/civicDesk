package com.civicdesk.servicerequest.dto.response;

import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceCatalogResponse {
    private Long serviceId;
    private String serviceName;
    private String departmentId;
    private ServiceCategory category;
    private Integer processingDays;
    private List<String> requiredDocuments;
    private BigDecimal fee;
    private ServiceStatus status;
    private LocalDateTime createdAt;
    private String message;
}
