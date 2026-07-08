package com.civicdesk.grievance.dto.request;

import java.sql.Date;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsTrendDto {

    private LocalDate date;

    private Long count;

    public AnalyticsTrendDto(Date date, Long count) {
        this.date = date == null ? null : date.toLocalDate();
        this.count = count;
    }
}
