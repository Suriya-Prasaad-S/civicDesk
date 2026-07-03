package com.civicdesk.grievance.entity;

import com.civicdesk.grievance.enums.ActionType;
import com.civicdesk.grievance.enums.GrievanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "grievance_actions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrievanceAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", nullable = false)
    private Grievance grievance;

    @Column(nullable = false)
    private Long actionTakenBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    private GrievanceStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private GrievanceStatus newStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
