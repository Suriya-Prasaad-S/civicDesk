package com.civicdesk.permit.entity;

import com.civicdesk.permit.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "permit_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermitDocument {

    @Id
    @Column(name = "document_id", length = 36)
    private String documentId;

    @Column(name = "permit_id", nullable = false)
    private Long permitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private String verificationStatus = "Pending";

    @Column(name = "verification_remarks", length = 255)
    private String verificationRemarks;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
