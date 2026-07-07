package com.civicdesk.permit.entity;

import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "permit_application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermitApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permit_id")
    private Long permitId;

    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    // userId from JWT — owner of this permit
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permit_type", nullable = false)
    private PermitType permitType;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "property_address", nullable = false)
    private String propertyAddress;

    // Duration in months (e.g. 12 = 1 year)
    @Column(name = "validity_period", nullable = false)
    private Integer validityPeriod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PermitStatus status = PermitStatus.APPLIED;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    // Expiry date computed from approval date + validity_period
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Which department is handling this permit
    @Column(name = "department_id")
    private Long departmentId;

    @OneToMany(mappedBy = "permitApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inspection> inspections;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
