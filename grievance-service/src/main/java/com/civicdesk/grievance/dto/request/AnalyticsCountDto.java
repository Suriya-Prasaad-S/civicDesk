package com.civicdesk.grievance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsCountDto {

    private String label;

    private Long count;

    public AnalyticsCountDto(Enum<?> label, Long count) {
        this.label = label == null ? null : label.name();
        this.count = count;
    }
}
