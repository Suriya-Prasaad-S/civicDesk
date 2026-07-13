package com.civicdesk.publicworks.entity;

import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.enums.WorkPriority;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
@Entity
@Table(name = "work_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLRestriction("is_deleted = false")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workOrderId;

    @Column(nullable = false)
    private String projectName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkCategory category;

    private Long departmentId;

    private String ward;

    private String zone;

    private String location;

    // userId of the assigned contractor / field officer
    private Long contractorId;

    // userId of the supervisor who assigned
    private Long assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkPriority priority = WorkPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.PLANNED;

    @Column(precision = 15, scale = 2)
    private BigDecimal budgetAllocated;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetConsumedTotal = BigDecimal.ZERO;

    private LocalDate startDate;

    private LocalDate expectedEndDate;

    private LocalDate actualEndDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Milestone> milestones;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
