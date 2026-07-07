package com.civicdesk.grievance.dto;

import com.civicdesk.grievance.enums.GrievanceCategory;
import com.civicdesk.grievance.enums.GrievancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrievanceRequest {

    @NotNull(message = "citizenId is required")
    private Long citizenId;

    @NotNull(message = "category is required")
    private GrievanceCategory category;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "description is required")
    private String description;

    private String location;

    private Long departmentId;

    private GrievancePriority priority;
}
