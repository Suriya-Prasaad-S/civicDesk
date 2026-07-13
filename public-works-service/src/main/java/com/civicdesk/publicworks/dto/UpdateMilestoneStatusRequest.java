package com.civicdesk.publicworks.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMilestoneStatusRequest {

    private String status;

    private String remarks;
}