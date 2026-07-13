package com.civicdesk.permit.service;

import com.civicdesk.permit.client.NotificationClient;
import com.civicdesk.permit.dto.*;
import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.entity.PermitDocument;
import com.civicdesk.permit.enums.DocumentType;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import com.civicdesk.permit.exception.BadRequestException;
import com.civicdesk.permit.exception.ForbiddenException;
import com.civicdesk.permit.exception.ResourceNotFoundException;
import com.civicdesk.permit.repository.InspectionRepository;
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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.civicdesk.permit.enums.InspectionOutcome;
import com.civicdesk.permit.enums.InspectionStatus;
import com.civicdesk.permit.client.AuditLogClient;
import java.util.HashMap;
import java.util.ArrayList;
@Service
@RequiredArgsConstructor
@Slf4j
public class PermitService {

    private final PermitApplicationRepository permitRepository;
    private final PermitDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final InspectionRepository inspectionRepository;
    private final NotificationClient notificationClient;
    private final AuditLogClient auditLogClient;

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

        /*PermitApplication saved = permitRepository.save(permit);
        log.info("Permit applied: permitId={} type={} userId={}", saved.getPermitId(), saved.getPermitType(), userId);
        return mapToResponse(saved);
         */

        PermitApplication saved = permitRepository.save(permit);

        try {

            NotificationRequest payload =
                    NotificationRequest.builder()
                            .userId(saved.getUserId())
                            .title("Permit Submitted")
                            .message(
                                    "Your permit application for "
                                            + saved.getPermitType()
                                            + " has been submitted successfully.")
                            .notificationType("PERMIT_UPDATE")
                            .referenceId(saved.getPermitId())
                            .referenceType("PERMIT")
                            .build();

            notificationClient.sendNotification(payload);
            auditLogClient.log(
        String.valueOf(userId),
        "CREATE_PERMIT",
        "PERMIT");

        } catch (Exception ex) {

            log.error(
                    "Notification dispatch failed: {}",
                    ex.getMessage());
        }

        log.info(
                "Permit applied: permitId={} type={} userId={}",
                saved.getPermitId(),
                saved.getPermitType(),
                userId);

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

        /*PermitApplication saved = permitRepository.save(permit);
        log.info("Permit renewal submitted: permitId={}", permitId);
        return mapToResponse(saved);

         */

        PermitApplication saved = permitRepository.save(permit);

        try {

            NotificationRequest payload =
                    NotificationRequest.builder()
                            .userId(saved.getUserId())
                            .title("Permit Renewal Submitted")
                            .message(
                                    "Your permit renewal request has been submitted successfully.")
                            .notificationType("PERMIT_UPDATE")
                            .referenceId(saved.getPermitId())
                            .referenceType("PERMIT")
                            .build();

            notificationClient.sendNotification(payload);
            auditLogClient.log(
        String.valueOf(userId),
        "RENEW_PERMIT",
        "PERMIT");

        } catch (Exception ex) {

            log.error(
                    "Notification dispatch failed: {}",
                    ex.getMessage());
        }

        log.info(
                "Permit renewal submitted: permitId={}",
                permitId);

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
        /*if (newStatus == PermitStatus.APPROVED) {
            permit.setExpiryDate(LocalDate.now().plusMonths(permit.getValidityPeriod()));
        }
         */

        if (newStatus == PermitStatus.APPROVED
                || newStatus == PermitStatus.REJECTED) {
            permit.setDecisionDate(LocalDate.now());
        }

        if (newStatus == PermitStatus.APPROVED) {
            permit.setExpiryDate(
                    LocalDate.now()
                            .plusMonths(permit.getValidityPeriod()));
        }


        /*PermitApplication saved = permitRepository.save(permit);
        log.info("Permit status updated: permitId={} status={}", permitId, newStatus);
        return mapToResponse(saved);

         */

        PermitApplication saved = permitRepository.save(permit);

        try {

            String title = null;
            String message = null;
            String type = "PERMIT_UPDATE";

            if (newStatus == PermitStatus.APPROVED) {

                title = "Permit Approved";

                message =
                        "Your permit application for "
                                + saved.getPermitType()
                                + " has been approved.";

            } else if (newStatus == PermitStatus.REJECTED) {

                title = "Permit Rejected";

                message =
                        "Your permit application for "
                                + saved.getPermitType()
                                + " has been rejected.";



            } else if (newStatus == PermitStatus.PENDING_DOCUMENTS) {

                title = "Additional Documents Required";

                message =
                        "Additional documents are required for your permit application.";


            }

            if (title != null) {

                NotificationRequest payload =
                        NotificationRequest.builder()
                                .userId(saved.getUserId())
                                .title(title)
                                .message(message)
                                .notificationType(type)
                                .referenceId(saved.getPermitId())
                                .referenceType("PERMIT")
                                .build();

                notificationClient.sendNotification(payload);
            }

        } catch (Exception ex) {

            log.error(
                    "Notification dispatch failed: {}",
                    ex.getMessage());
        }

        log.info(
                "Permit status updated: permitId={} status={}",
                permitId,
                newStatus);

                if (newStatus == PermitStatus.APPROVED) {

    auditLogClient.log(
            String.valueOf(saved.getUserId()),
            "APPROVE_PERMIT",
            "PERMIT");

} else if (newStatus == PermitStatus.REJECTED) {

    auditLogClient.log(
            String.valueOf(saved.getUserId()),
            "REJECT_PERMIT",
            "PERMIT");

} else if (newStatus == PermitStatus.PENDING_DOCUMENTS) {

    auditLogClient.log(
            String.valueOf(saved.getUserId()),
            "REQUEST_DOCUMENTS",
            "PERMIT");
}

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
            auditLogClient.log(
        String.valueOf(permit.getUserId()),
        "UPLOAD_DOCUMENT",
        "PERMIT");
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
        auditLogClient.log(
        String.valueOf(permitId),
        "VERIFY_DOCUMENT",
        "PERMIT");
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
            case APPLIED             -> next == PermitStatus.UNDER_REVIEW ||  next == PermitStatus.INSPECTION_SCHEDULED ||       next == PermitStatus.REJECTED;
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

    public PermitAnalyticsResponse getPermitAnalytics(
            LocalDate fromDate,
            LocalDate toDate) {

        PermitAnalyticsResponse response =
                new PermitAnalyticsResponse();

        response.setTotalPermits(
                permitRepository.countPermits(
                        fromDate,
                        toDate));

        response.setStatusBreakdown(
                buildPermitStatusBreakdown(
                        fromDate,
                        toDate));

        response.setPermitTypeBreakdown(
                buildPermitTypeBreakdown(
                        fromDate,
                        toDate));

        response.setApplicationTrend(
                buildApplicationTrend(
                        fromDate,
                        toDate));

        response.setDecisionTrend(
                buildDecisionTrend(
                        fromDate,
                        toDate));

        response.setAverageDecisionDays(
                calculateAverageDecisionDays());

        PermitAnalyticsResponse.InspectionAnalytics inspection =
                new PermitAnalyticsResponse.InspectionAnalytics();

        inspection.setStatusBreakdown(
                buildInspectionStatusBreakdown());

        inspection.setOutcomeBreakdown(
                buildInspectionOutcomeBreakdown());

        response.setInspection(inspection);

        return response;
    }

    private List<AnalyticsLabelCountDto> buildPermitStatusBreakdown(
            LocalDate fromDate,
            LocalDate toDate) {

        List<AnalyticsLabelCountResponse> dbValues =
                permitRepository.getStatusBreakdown(
                        fromDate,
                        toDate,
                        AnalyticsLabelCountResponse.class);

        Map<String, Long> countMap =
                dbValues.stream()
                        .collect(Collectors.toMap(
                                AnalyticsLabelCountResponse::getLabel,
                                AnalyticsLabelCountResponse::getCount));

        return Arrays.stream(PermitStatus.values())
                .map(status ->
                        new AnalyticsLabelCountDto(
                                status.name(),
                                countMap.getOrDefault(
                                        status.name(),
                                        0L)))
                .toList();
    }

    private List<AnalyticsLabelCountDto> buildPermitTypeBreakdown(
            LocalDate fromDate,
            LocalDate toDate) {

        List<AnalyticsLabelCountResponse> dbValues =
                permitRepository.getPermitTypeBreakdown(
                        fromDate,
                        toDate,
                        AnalyticsLabelCountResponse.class);

        Map<String, Long> countMap =
                dbValues.stream()
                        .collect(Collectors.toMap(
                                AnalyticsLabelCountResponse::getLabel,
                                AnalyticsLabelCountResponse::getCount));

        return Arrays.stream(PermitType.values())
                .map(type ->
                        new AnalyticsLabelCountDto(
                                type.name(),
                                countMap.getOrDefault(
                                        type.name(),
                                        0L)))
                .toList();
    }

    private List<AnalyticsLabelCountDto> buildInspectionStatusBreakdown() {

        List<AnalyticsLabelCountResponse> dbValues =
                inspectionRepository.getStatusBreakdown(
                        AnalyticsLabelCountResponse.class);

        Map<String, Long> countMap =
                dbValues.stream()
                        .collect(Collectors.toMap(
                                AnalyticsLabelCountResponse::getLabel,
                                AnalyticsLabelCountResponse::getCount));

        return Arrays.stream(InspectionStatus.values())
                .map(status ->
                        new AnalyticsLabelCountDto(
                                status.name(),
                                countMap.getOrDefault(
                                        status.name(),
                                        0L)))
                .toList();
    }

    private List<AnalyticsLabelCountDto> buildInspectionOutcomeBreakdown() {

        List<AnalyticsLabelCountResponse> dbValues =
                inspectionRepository.getOutcomeBreakdown(
                        AnalyticsLabelCountResponse.class);

        Map<String, Long> countMap =
                dbValues.stream()
                        .collect(Collectors.toMap(
                                AnalyticsLabelCountResponse::getLabel,
                                AnalyticsLabelCountResponse::getCount));

        return Arrays.stream(InspectionOutcome.values())
                .map(outcome ->
                        new AnalyticsLabelCountDto(
                                outcome.name(),
                                countMap.getOrDefault(
                                        outcome.name(),
                                        0L)))
                .toList();
    }

    private Double calculateAverageDecisionDays() {

        List<PermitApplication> permits =
                permitRepository.getDecidedPermits();

        if (permits.isEmpty()) {
            return 0.0;
        }

        return permits.stream()
                .mapToLong(p ->
                        java.time.temporal.ChronoUnit.DAYS.between(
                                p.getApplicationDate(),
                                p.getDecisionDate()))
                .average()
                .orElse(0.0);
    }

    private List<AnalyticsTrendDto> buildApplicationTrend(
            LocalDate fromDate,
            LocalDate toDate) {

        List<AnalyticsTrendResponse> dbTrend =
                permitRepository.getApplicationTrend(
                        fromDate,
                        toDate,
                        AnalyticsTrendResponse.class);

        Map<LocalDate, Long> countMap =
                dbTrend.stream()
                        .collect(Collectors.toMap(
                                AnalyticsTrendResponse::getDate,
                                AnalyticsTrendResponse::getCount));

        List<AnalyticsTrendDto> result =
                new ArrayList<>();

        LocalDate current = fromDate;

        while (!current.isAfter(toDate)) {

            result.add(
                    new AnalyticsTrendDto(
                            current.toString(),
                            countMap.getOrDefault(
                                    current,
                                    0L)));

            current = current.plusDays(1);
        }

        return result;
    }

    private List<AnalyticsTrendDto> buildDecisionTrend(
            LocalDate fromDate,
            LocalDate toDate) {

        List<AnalyticsTrendResponse> dbTrend =
                permitRepository.getDecisionTrend(
                        AnalyticsTrendResponse.class);

        Map<LocalDate, Long> countMap =
                dbTrend.stream()
                        .filter(x ->
                                x.getDate() != null
                                        && !x.getDate().isBefore(fromDate)
                                        && !x.getDate().isAfter(toDate))
                        .collect(Collectors.toMap(
                                AnalyticsTrendResponse::getDate,
                                AnalyticsTrendResponse::getCount));

        List<AnalyticsTrendDto> result =
                new ArrayList<>();

        LocalDate current = fromDate;

        while (!current.isAfter(toDate)) {

            result.add(
                    new AnalyticsTrendDto(
                            current.toString(),
                            countMap.getOrDefault(
                                    current,
                                    0L)));

            current = current.plusDays(1);
        }

        return result;
    }
}
