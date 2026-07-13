package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.client.AuditLogClient;
import com.civicdesk.servicerequest.client.NotificationClient;
import com.civicdesk.servicerequest.dto.request.NotificationRequest;
import com.civicdesk.servicerequest.dto.response.RequestDocumentResponse;
import com.civicdesk.servicerequest.dto.response.DocumentItemResponse;
import com.civicdesk.servicerequest.dto.response.MessageResponse;
import com.civicdesk.servicerequest.entity.RequestDocument;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.enums.VerificationStatus;
import com.civicdesk.servicerequest.exception.BadRequestException;
import com.civicdesk.servicerequest.exception.ForbiddenException;
import com.civicdesk.servicerequest.exception.ResourceNotFoundException;
import com.civicdesk.servicerequest.repository.RequestDocumentRepository;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import com.civicdesk.servicerequest.security.JwtUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestDocumentService {

    private final RequestDocumentRepository documentRepository;
    private final ServiceRequestRepository requestRepository;
    private final NotificationClient notificationClient;
    private final AuditLogClient auditLogClient;

    @Transactional
    public MessageResponse uploadDocument(Long requestId, String documentType, MultipartFile file, Long userId) {
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found. No request exists with the given requestId."));

        if (!serviceRequest.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only upload documents to your own requests.");
        }

        if (serviceRequest.getStatus() == RequestStatus.COMPLETED ||
            serviceRequest.getStatus() == RequestStatus.REJECTED) {
            throw new BadRequestException("Upload not allowed. Cannot upload documents to a Completed or Rejected request.");
        }

        String filePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads/service-request-docs");
                Files.createDirectories(uploadDir);
                String uuid8 = UUID.randomUUID().toString().substring(0, 8);
                String filename = serviceRequest.getUserId() + "_" + uuid8 + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), uploadDir.resolve(filename));
                filePath = filename;
            } catch (IOException e) {
                throw new BadRequestException("Failed to store file: " + e.getMessage());
            }
        }

        RequestDocument doc = RequestDocument.builder()
                .serviceRequest(serviceRequest)
                .documentType(documentType)
                .filePath(filePath)
                .uploadedDate(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        RequestDocument saved = documentRepository.save(doc);
        log.info("Document uploaded: docId={} requestId={}", saved.getDocSubmissionId(), requestId);

        boolean transitionedToUnderReview = false;
        if (serviceRequest.getStatus() == RequestStatus.PENDING_DOCUMENTS) {
            serviceRequest.setStatus(RequestStatus.UNDER_REVIEW);
            requestRepository.save(serviceRequest);
            transitionedToUnderReview = true;
            log.info("Request status transitioned from PENDING_DOCUMENTS to UNDER_REVIEW due to document upload: requestId={}", requestId);
        }

        try {
            String uploadMessage = transitionedToUnderReview
                    ? "Your document '" + documentType + "' was uploaded successfully. Request status has been moved to Under Review."
                    : "Your document '" + documentType + "' was uploaded successfully and is pending review.";
            NotificationRequest notificationPayload = NotificationRequest.builder()
                    .userId(serviceRequest.getUserId())
                    .title("Document Uploaded")
                    .message(uploadMessage)
                    .notificationType("SERVICE_REQUEST_UPDATE")
                    .referenceId(serviceRequest.getRequestId())
                    .referenceType("SERVICE_REQUEST")
                    .build();
            notificationClient.sendNotification(notificationPayload);
        } catch (Exception ex) {
            log.error("Failed to send document upload notification: {}", ex.getMessage());
        }

        DocumentItemResponse item = mapToDocumentItem(saved);
        auditLogClient.log(String.valueOf(userId), "UPLOAD_DOCUMENT", "CITIZEN");
        return MessageResponse.builder()
                .message("Document uploaded successfully")
                .id(saved.getDocSubmissionId())
                .data(item)
                .build();
    }

    public List<DocumentItemResponse> getByRequestId(Long requestId, Long userId, String role) {
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found. No request exists with the given requestId."));

        if ("CIT".equals(role) && !serviceRequest.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied. You are not authorized to view these documents.");
        }

        return documentRepository.findByServiceRequest_RequestId(requestId)
            .stream().map(this::mapToDocumentItem).toList();
    }

    @Transactional
    public MessageResponse verifyDocument(Long docId, VerificationStatus status) {
        RequestDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found. No document exists with the given docId."));

        doc.setVerificationStatus(status);
        RequestDocument updated = documentRepository.save(doc);
        log.info("Document verified: docId={} status={}", docId, status);

        ServiceRequest serviceRequest = doc.getServiceRequest();
        boolean requestMovedToPendingDocuments = false;
        if (status == VerificationStatus.REJECTED && serviceRequest.getStatus() == RequestStatus.UNDER_REVIEW) {
            serviceRequest.setStatus(RequestStatus.PENDING_DOCUMENTS);
            requestRepository.save(serviceRequest);
            requestMovedToPendingDocuments = true;
            log.info("Request status transitioned from UNDER_REVIEW to PENDING_DOCUMENTS due to document rejection: requestId={}", serviceRequest.getRequestId());
        }

        NotificationRequest notificationPayload = null;
        if (status == VerificationStatus.REJECTED) {
            String rejectionMessage = requestMovedToPendingDocuments
                    ? "Your document '" + doc.getDocumentType() + "' was rejected. Request status has been moved to Pending Documents. Please upload a new document."
                    : "Your document '" + doc.getDocumentType() + "' was rejected. Please upload a new document.";
            notificationPayload = NotificationRequest.builder()
                    .userId(serviceRequest.getUserId())
                    .title("Document Rejected")
                    .message(rejectionMessage)
                    .notificationType("SERVICE_REQUEST_UPDATE")
                    .referenceId(serviceRequest.getRequestId())
                    .referenceType("SERVICE_REQUEST")
                    .build();
        } else if (status == VerificationStatus.VERIFIED) {
            notificationPayload = NotificationRequest.builder()
                    .userId(serviceRequest.getUserId())
                    .title("Document Approved")
                    .message("Your document '" + doc.getDocumentType() + "' has been approved successfully.")
                    .notificationType("SERVICE_REQUEST_UPDATE")
                    .referenceId(serviceRequest.getRequestId())
                    .referenceType("SERVICE_REQUEST")
                    .build();
        }

        if (notificationPayload != null) {
            try {
                notificationClient.sendNotification(notificationPayload);
            } catch (Exception ex) {
                log.error("Failed to send document verification notification: {}", ex.getMessage());
            }
        }

        DocumentItemResponse item = mapToDocumentItem(updated);
        String message;
        if (status == VerificationStatus.VERIFIED) {
            message = "Document verified successfully. Document status has been updated to Verified.";
        } else {
            message = "Document rejected. Document status has been set to Rejected. Request status has been moved to PendingDocuments. Citizen has been notified to re-upload.";
        }
        Long currentUserId = JwtUserContext.getCurrentUserId();
        String actorUserIdStr = currentUserId != null ? String.valueOf(currentUserId) : "SYSTEM";
        auditLogClient.log(actorUserIdStr, "VERIFY_DOCUMENT", "CITIZEN");

        return MessageResponse.builder()
            .message(message)
            .id(updated.getDocSubmissionId())
            .data(item)
            .build();
    }

    private DocumentItemResponse mapToDocumentItem(RequestDocument doc) {
        return DocumentItemResponse.builder()
                .documentId(doc.getDocSubmissionId())
                .requestId(doc.getServiceRequest().getRequestId())
                .fileName(doc.getFilePath())
                .fileType(null)
                .fileSize(0L)
                .downloadUrl("/downloads/" + doc.getFilePath())
                .verificationStatus(doc.getVerificationStatus())
                .verificationRemarks(null)
                .build();
    }
}
