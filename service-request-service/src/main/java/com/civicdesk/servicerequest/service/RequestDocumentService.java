package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.RequestDocumentResponse;
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
    public RequestDocumentResponse uploadDocument(Long requestId, String documentType, MultipartFile file, Long userId) {
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

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
        return mapToResponse(saved);
    }

    public List<RequestDocumentResponse> getByRequestId(Long requestId, Long userId, String role) {
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        if ("CIT".equals(role) && !serviceRequest.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied.");
        }

        return documentRepository.findByServiceRequest_RequestId(requestId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public RequestDocumentResponse verifyDocument(Long docId, VerificationStatus status) {
        RequestDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found. No document exists with the given docId."));

        doc.setVerificationStatus(status);
        RequestDocument updated = documentRepository.save(doc);
        log.info("Document verified: docId={} status={}", docId, status);
        return mapToResponse(updated);
    }

    private RequestDocumentResponse mapToResponse(RequestDocument doc) {
        return RequestDocumentResponse.builder()
                .docSubmissionId(doc.getDocSubmissionId())
                .requestId(doc.getServiceRequest().getRequestId())
                .documentType(doc.getDocumentType())
                .filePath(doc.getFilePath())
                .uploadedDate(doc.getUploadedDate())
                .verificationStatus(doc.getVerificationStatus())
                .build();
    }
}
