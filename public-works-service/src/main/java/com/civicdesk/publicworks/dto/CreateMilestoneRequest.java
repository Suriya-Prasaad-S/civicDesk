package com.civicdesk.publicworks.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateMilestoneRequest {

    private String description;

    private LocalDate plannedDate;

    private String remarks;
}
