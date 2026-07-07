package com.civicdesk.grievance.dto;

import com.civicdesk.grievance.enums.ActionType;
import com.civicdesk.grievance.enums.GrievanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class GrievanceActionResponse {
    private Long actionId;
    private Long grievanceId;
    private Long actionTakenBy;
    private ActionType actionType;
    private String remarks;
    private GrievanceStatus oldStatus;
    private GrievanceStatus newStatus;
    private LocalDateTime createdAt;
}
