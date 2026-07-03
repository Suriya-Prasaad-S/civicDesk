package com.civicdesk.servicerequest.entity;

import com.civicdesk.servicerequest.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_submission_id")
    private Long docSubmissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
