package com.civicdesk.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateReportRequest {

    private String departmentId;

    @NotBlank
    private String type;

    @NotNull
    private LocalDateTime fromDate;

    @NotNull
    private LocalDateTime toDate;
}
