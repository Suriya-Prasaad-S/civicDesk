package com.civicdesk.notification.entity;

import com.civicdesk.notification.enums.NotificationType;
import com.civicdesk.notification.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private NotificationType notificationType;

    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private ReferenceType referenceType = ReferenceType.NONE;

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private Boolean isDismissed = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
