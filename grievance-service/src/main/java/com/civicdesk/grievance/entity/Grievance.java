package com.civicdesk.grievance.entity;

import com.civicdesk.grievance.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "grievances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long grievanceId;

    @Column(nullable = false)
    private Long citizenId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievanceCategory category;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String location;

    private Long departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GrievancePriority priority = GrievancePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GrievanceStatus status = GrievanceStatus.SUBMITTED;

    private Long assignedTo;

    @Column(nullable = false)
    private LocalDate submittedDate;

    private LocalDate resolvedDate;

    @Column(nullable = false)
    private LocalDate slaDeadline;

    private Boolean slaBreach;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EscalationLevel escalationLevel = EscalationLevel.L1;

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GrievanceAction> actions;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
