package com.civicdesk.permit.dto;

import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PermitApplicationResponse {
    private Long permitId;
    private Long citizenId;
    private Long userId;
    private PermitType permitType;
    private LocalDate applicationDate;
    private String propertyAddress;
    private Integer validityPeriod;
    private BigDecimal fee;
    private PermitStatus status;
    private String remarks;
    private LocalDate expiryDate;
    private Long departmentId;
    private LocalDateTime createdAt;
}
