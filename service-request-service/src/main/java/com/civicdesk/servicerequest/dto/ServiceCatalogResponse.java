package com.civicdesk.servicerequest.dto;

import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceCatalogResponse {
    private Long serviceId;
    private String serviceName;
    private Long departmentId;
    private ServiceCategory category;
    private Integer processingDays;
    private String requiredDocuments;
    private BigDecimal fee;
    private ServiceStatus status;
    private LocalDateTime createdAt;
}
