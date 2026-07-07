package com.civicdesk.permit.dto;

import com.civicdesk.permit.enums.InspectionOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ConductInspectionRequest {

    @NotNull(message = "Outcome is required")
    private InspectionOutcome outcome;

    @NotNull(message = "Conducted date is required")
    private LocalDate conductedDate;

    private String remarks;

    // Stored as "lat,lng" string per project spec
    private String geoCoordinates;
}
