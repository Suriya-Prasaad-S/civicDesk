package com.civicdesk.permit.dto;

import com.civicdesk.permit.enums.InspectionOutcome;
import com.civicdesk.permit.enums.InspectionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InspectionResponse {
    private Long inspectionId;
    private Long permitId;
    private Long assignedOfficerId;
    private LocalDate scheduledDate;
    private LocalDate conductedDate;
    private InspectionOutcome outcome;
    private String remarks;
    private InspectionStatus status;
    private String geoCoordinates;
    private LocalDateTime createdAt;
}
