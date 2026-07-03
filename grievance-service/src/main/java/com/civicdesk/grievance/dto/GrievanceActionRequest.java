package com.civicdesk.grievance.dto;

import lombok.Data;

@Data
public class GrievanceActionRequest {
    private String actionType;
    private String description;
    private String actionDate;
}
