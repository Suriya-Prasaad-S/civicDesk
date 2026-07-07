package com.civicdesk.grievance.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.civicdesk.grievance.enums.Category;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;

/** Lightweight row for the citizen's grievance list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceSummaryResponse {

    private String grievanceId;
    private String grievanceTitle;
    private Category category;
    private GrievanceStatus status;
    private EscalationLevel escalationLevel;
    private LocalDateTime submissionDate;
}
