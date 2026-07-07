package com.civicdesk.permit.service;

import com.civicdesk.permit.dto.DocumentResponse;
import com.civicdesk.permit.dto.PermitApplicationRequest;
import com.civicdesk.permit.dto.PermitApplicationResponse;
import com.civicdesk.permit.dto.RenewPermitRequest;
import com.civicdesk.permit.dto.VerifyDocumentRequest;
import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.entity.PermitDocument;
import com.civicdesk.permit.enums.DocumentType;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import com.civicdesk.permit.exception.BadRequestException;
import com.civicdesk.permit.exception.ForbiddenException;
import com.civicdesk.permit.exception.ResourceNotFoundException;
import com.civicdesk.permit.repository.PermitApplicationRepository;
import com.civicdesk.permit.repository.PermitDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermitService {

    private final PermitApplicationRepository permitRepository;
    private final PermitDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    // ─── CITIZEN ─────────────────────────────────────────────────────────────

    @Transactional
    public PermitApplicationResponse applyForPermit(PermitApplicationRequest request, Long userId) {
        Long citizenId = request.getCitizenId() != null ? request.getCitizenId() : userId;
        PermitApplication permit = PermitApplication.builder()
                .citizenId(citizenId)
                .userId(userId)
                .permitType(request.getPermitType())
                .applicationDate(LocalDate.now())
                .propertyAddress(request.getPropertyAddress())
                .validityPeriod(request.getValidityPeriod())
                .fee(request.getFee())
                .departmentId(request.getDepartmentId())
                .status(PermitStatus.APPLIED)
                .build();

        PermitApplication saved = permitRepository.save(permit);
        log.info("Permit applied: permitId={} type={} userId={}", saved.getPermitId(), saved.getPermitType(), userId);
        return mapToResponse(saved);
    }

    public List<PermitApplicationResponse> getMyPermits(Long userId) {
        return permitRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public PermitApplicationResponse renewPermit(Long permitId, RenewPermitRequest request, Long userId) {
        PermitApplication permit = getEntityById(permitId);

        if (!permit.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only renew your own permits.");
        }
        if (permit.getStatus() != PermitStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED permits can be renewed. Current status: " + permit.getStatus());
        }

        // Reset to applied state for new review cycle
        permit.setStatus(PermitStatus.APPLIED);
        permit.setValidityPeriod(request.getValidityPeriod());
        permit.setApplicationDate(LocalDate.now());
        permit.setExpiryDate(null);
        permit.setRemarks("Renewal application submitted.");

        PermitApplication saved = permitRepository.save(permit);
        log.info("Permit renewal submitted: permitId={}", permitId);
        return mapToResponse(saved);
    }

    // ─── STAFF ───────────────────────────────────────────────────────────────

    public PermitApplicationResponse getById(Long permitId, Long userId, String role) {
        PermitApplication permit = getEntityById(permitId);
        if ("CIT".equals(role) && !permit.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied. You can only view your own permits.");
        }
        return mapToResponse(permit);
    }

    public List<PermitApplicationResponse> getAll() {
        return permitRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<PermitApplicationResponse> getByStatus(PermitStatus status) {
        return permitRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    public List<PermitApplicationResponse> getByDepartment(Long departmentId) {
        return permitRepository.findByDepartmentId(departmentId).stream().map(this::mapToResponse).toList();
    }

    public List<PermitApplicationResponse> getByDepartmentAndStatus(Long departmentId, PermitStatus status) {
        return permitRepository.findByStatusAndDepartmentId(status, departmentId)
                .stream().map(this::mapToResponse).toList();
    }

    public List<PermitApplicationResponse> getByType(PermitType type) {
        return permitRepository.findByPermitType(type).stream().map(this::mapToResponse).toList();
    }

    public List<PermitApplicationResponse> getExpiringSoon(int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return permitRepository.findByStatusAndExpiryDateBefore(PermitStatus.APPROVED, cutoff)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public PermitApplicationResponse updateStatus(Long permitId, PermitStatus newStatus, String remarks) {
        PermitApplication permit = getEntityById(permitId);
        validateStatusTransition(permit.getStatus(), newStatus);

        permit.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            permit.setRemarks(remarks);
        }

        // Set expiry date when approved
        if (newStatus == PermitStatus.APPROVED) {
            permit.setExpiryDate(LocalDate.now().plusMonths(permit.getValidityPeriod()));
        }

        PermitApplication saved = permitRepository.save(permit);
        log.info("Permit status updated: permitId={} status={}", permitId, newStatus);
        return mapToResponse(saved);
    }

    // Called internally by InspectionService after inspection outcome
    @Transactional
    public void applyInspectionOutcome(Long permitId, com.civicdesk.permit.enums.InspectionOutcome outcome) {
        PermitApplication permit = getEntityById(permitId);
        switch (outcome) {
            case PASS -> {
                permit.setStatus(PermitStatus.APPROVED);
                permit.setExpiryDate(LocalDate.now().plusMonths(permit.getValidityPeriod()));
                permit.setRemarks("Site inspection passed. Permit approved.");
            }
            case FAIL -> {
                permit.setStatus(PermitStatus.REJECTED);
                permit.setRemarks("Site inspection failed. Permit rejected.");
            }
            case CONDITIONAL_APPROVAL -> {
                permit.setStatus(PermitStatus.UNDER_REVIEW);
                permit.setRemarks("Conditional approval from inspection. Pending supervisor decision.");
            }
        }
        permitRepository.save(permit);
        log.info("Inspection outcome applied to permit: permitId={} outcome={}", permitId, outcome);
    }

    // ─── DOCUMENTS ───────────────────────────────────────────────────────────

    @Transactional
    public void uploadDocuments(Long permitId, List<String> documentTypes, List<MultipartFile> files) {
        PermitApplication permit = getEntityById(permitId);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String documentTypeStr = documentTypes.get(i);

            DocumentType docType;
            try {
                docType = DocumentType.valueOf(documentTypeStr);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid documentType: " + documentTypeStr
                        + ". Valid values: IDProof, SitePlan, NOC, TradeCertificate, EventLayout, PropertyDeed, FireNOC, IndemnityBond");
            }

            String filePath = fileStorageService.store(file, "permits/" + permitId);

            PermitDocument doc = PermitDocument.builder()
                    .documentId(UUID.randomUUID().toString())
                    .permitId(permitId)
                    .documentType(docType)
                    .filePath(filePath)
                    .verificationStatus("Pending")
                    .isDeleted(false)
                    .build();
            documentRepository.save(doc);
            log.info("Document uploaded: permitId={} type={} file={}", permitId, docType, filePath);
        }

        if (permit.getStatus() == PermitStatus.APPLIED || permit.getStatus() == PermitStatus.PENDING_DOCUMENTS) {
            permit.setStatus(PermitStatus.UNDER_REVIEW);
            permitRepository.save(permit);
        }
    }

    public List<DocumentResponse> getDocuments(Long permitId) {
        getEntityById(permitId);
        return documentRepository.findByPermitIdAndIsDeletedFalse(permitId)
                .stream()
                .map(d -> {
                    DocumentResponse r = new DocumentResponse();
                    r.setDocumentId(d.getDocumentId());
                    r.setDocumentType(d.getDocumentType().name());
                    r.setFilePath(d.getFilePath());
                    r.setVerificationStatus(d.getVerificationStatus());
                    r.setVerificationRemarks(d.getVerificationRemarks());
                    r.setUploadedAt(d.getUploadedAt());
                    return r;
                })
                .toList();
    }

    @Transactional
    public void verifyDocument(Long permitId, String documentId, VerifyDocumentRequest req) {
        getEntityById(permitId);
        PermitDocument doc = documentRepository.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        doc.setVerificationStatus(req.getVerificationStatus());
        doc.setVerificationRemarks(req.getVerificationRemarks());
        documentRepository.save(doc);
        log.info("Document verified: documentId={} status={}", documentId, req.getVerificationStatus());
    }

    public byte[] downloadDocument(String documentId) {
        PermitDocument doc = documentRepository.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        String filePath = doc.getFilePath();
        String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        try {
            return Files.readAllBytes(Paths.get(relativePath));
        } catch (IOException e) {
            throw new ResourceNotFoundException("File not found on server: " + filePath);
        }
    }

    public String getDocumentFileName(String documentId) {
        PermitDocument doc = documentRepository.findByDocumentIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        String filePath = doc.getFilePath();
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void validateStatusTransition(PermitStatus current, PermitStatus next) {
        boolean valid = switch (current) {
            case APPLIED             -> next == PermitStatus.UNDER_REVIEW || next == PermitStatus.REJECTED;
            case UNDER_REVIEW        -> next == PermitStatus.INSPECTION_SCHEDULED
                                     || next == PermitStatus.PENDING_DOCUMENTS
                                     || next == PermitStatus.APPROVED
                                     || next == PermitStatus.REJECTED;
            case PENDING_DOCUMENTS   -> next == PermitStatus.UNDER_REVIEW
                                     || next == PermitStatus.REJECTED;
            case INSPECTION_SCHEDULED-> next == PermitStatus.UNDER_REVIEW
                                     || next == PermitStatus.APPROVED
                                     || next == PermitStatus.REJECTED;
            case APPROVED            -> next == PermitStatus.EXPIRED;
            case REJECTED, EXPIRED   -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid permit status transition: %s → %s", current, next));
        }
    }

    public PermitApplication getEntityById(Long permitId) {
        return permitRepository.findById(permitId)
                .orElseThrow(() -> new ResourceNotFoundException("Permit not found with id: " + permitId));
    }

    private PermitApplicationResponse mapToResponse(PermitApplication p) {
        return PermitApplicationResponse.builder()
                .permitId(p.getPermitId())
                .citizenId(p.getCitizenId())
                .userId(p.getUserId())
                .permitType(p.getPermitType())
                .applicationDate(p.getApplicationDate())
                .propertyAddress(p.getPropertyAddress())
                .validityPeriod(p.getValidityPeriod())
                .fee(p.getFee())
                .status(p.getStatus())
                .remarks(p.getRemarks())
                .expiryDate(p.getExpiryDate())
                .departmentId(p.getDepartmentId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
