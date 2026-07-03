package com.civicdesk.analytics.entity;

import com.civicdesk.analytics.enums.ReportStatus;
import com.civicdesk.analytics.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "civic_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CivicReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private Long generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATED;

    // JSON string of the query parameters used
    @Column(columnDefinition = "TEXT")
    private String parameters;

    // JSON string of the report result snapshot
    @Column(columnDefinition = "LONGTEXT")
    private String reportData;

    @CreationTimestamp
    private LocalDateTime generatedAt;
}
