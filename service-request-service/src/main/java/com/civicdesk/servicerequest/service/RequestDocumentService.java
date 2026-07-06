package com.civicdesk.servicerequest.service;

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
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
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

        if (serviceRequest.getStatus() == RequestStatus.PENDING_DOCUMENTS) {
            serviceRequest.setStatus(RequestStatus.UNDER_REVIEW);
            requestRepository.save(serviceRequest);
            log.info("Request status transitioned from PENDING_DOCUMENTS to UNDER_REVIEW due to document upload: requestId={}", requestId);
        }

        DocumentItemResponse item = mapToDocumentItem(saved);
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
        if (status == VerificationStatus.REJECTED && serviceRequest.getStatus() == RequestStatus.UNDER_REVIEW) {
            serviceRequest.setStatus(RequestStatus.PENDING_DOCUMENTS);
            requestRepository.save(serviceRequest);
            log.info("Request status transitioned from UNDER_REVIEW to PENDING_DOCUMENTS due to document rejection: requestId={}", serviceRequest.getRequestId());
        }
        
        DocumentItemResponse item = mapToDocumentItem(updated);
        String message;
        if (status == VerificationStatus.VERIFIED) {
            message = "Document verified successfully. Document status has been updated to Verified.";
        } else {
            message = "Document rejected. Document status has been set to Rejected. Request status has been moved to PendingDocuments. Citizen has been notified to re-upload.";
        }
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
