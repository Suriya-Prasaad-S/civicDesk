package com.civicdesk.grievance.dto;

import com.civicdesk.grievance.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class GrievanceResponse {
    private Long grievanceId;
    private Long citizenId;
    private Long userId;
    private GrievanceCategory category;
    private String subject;
    private String description;
    private String location;
    private Long departmentId;
    private GrievancePriority priority;
    private GrievanceStatus status;
    private Long assignedTo;
    private LocalDate submittedDate;
    private LocalDate resolvedDate;
    private LocalDate slaDeadline;
    private Boolean slaBreach;
    private EscalationLevel escalationLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
