package com.civicdesk.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.civicdesk.analytics.util.NumericStringSequenceGenerator;

@Entity
@Table(name = "civic_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CivicReport {

    @Id
    @GeneratedValue(generator = "reportIdSeq")
    @GenericGenerator(
            name = "reportIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                    @org.hibernate.annotations.Parameter(
                            name = "sequence_name",
                            value = "report_id_seq"
                    ),
                    @org.hibernate.annotations.Parameter(
                            name = "initial_value",
                            value = "90000001"
                    ),
                    @org.hibernate.annotations.Parameter(
                            name = "increment_size",
                            value = "1"
                    ),
                    @org.hibernate.annotations.Parameter(
                            name = "optimizer",
                            value = "none"
                    )
            })
    @Column(name = "report_id", length = 36, updatable = false, nullable = false)
    private String reportId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "from_date")
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    @Column(name = "metrics", columnDefinition = "LONGTEXT")
    @Convert(converter = MetricsConverter.class)
    private Map<String, Object> metrics;

    @Column(name = "generated_date")
    private LocalDateTime generatedDate;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (reportId == null) {
            reportId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
