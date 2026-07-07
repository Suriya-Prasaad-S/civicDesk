package com.civicdesk.permit.service;

import com.civicdesk.permit.dto.ConductInspectionRequest;
import com.civicdesk.permit.dto.InspectionResponse;
import com.civicdesk.permit.dto.ScheduleInspectionRequest;
import com.civicdesk.permit.entity.Inspection;
import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.InspectionStatus;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.exception.BadRequestException;
import com.civicdesk.permit.exception.ForbiddenException;
import com.civicdesk.permit.exception.ResourceNotFoundException;
import com.civicdesk.permit.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final PermitService permitService;

    // ─── SUPERVISOR ──────────────────────────────────────────────────────────

    @Transactional
    public InspectionResponse scheduleInspection(Long permitId, ScheduleInspectionRequest request) {
        PermitApplication permit = permitService.getEntityById(permitId);

        if (permit.getStatus() != PermitStatus.APPLIED && permit.getStatus() != PermitStatus.UNDER_REVIEW) {
            throw new BadRequestException(
                    "Inspection can only be scheduled for APPLIED or UNDER_REVIEW permits. Current: " + permit.getStatus());
        }

        Inspection inspection = Inspection.builder()
                .permitApplication(permit)
                .assignedOfficerId(request.getOfficerId())
                .scheduledDate(request.getScheduledDate())
                .status(InspectionStatus.SCHEDULED)
                .build();

        Inspection saved = inspectionRepository.save(inspection);

        // Move permit status to INSPECTION_SCHEDULED
        permitService.updateStatus(permitId, PermitStatus.INSPECTION_SCHEDULED,
                "Inspection scheduled for " + request.getScheduledDate());

        log.info("Inspection scheduled: inspectionId={} permitId={} officerId={}",
                saved.getInspectionId(), permitId, request.getOfficerId());
        return mapToResponse(saved);
    }

    @Transactional
    public InspectionResponse cancelInspection(Long inspectionId) {
        Inspection inspection = getEntityById(inspectionId);

        if (inspection.getStatus() != InspectionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED inspections can be cancelled.");
        }

        inspection.setStatus(InspectionStatus.CANCELLED);
        Inspection saved = inspectionRepository.save(inspection);

        // Revert permit back to UNDER_REVIEW
        permitService.updateStatus(inspection.getPermitApplication().getPermitId(),
                PermitStatus.UNDER_REVIEW, "Inspection cancelled. Returned to review queue.");

        log.info("Inspection cancelled: inspectionId={}", inspectionId);
        return mapToResponse(saved);
    }

    // ─── FIELD OFFICER ────────────────────────────────────────────────────────

    @Transactional
    public InspectionResponse conductInspection(Long inspectionId,
                                                 ConductInspectionRequest request,
                                                 Long officerId) {
        Inspection inspection = getEntityById(inspectionId);

        if (!inspection.getAssignedOfficerId().equals(officerId)) {
            throw new ForbiddenException("You can only conduct inspections assigned to you.");
        }
        if (inspection.getStatus() != InspectionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED inspections can be conducted.");
        }

        inspection.setOutcome(request.getOutcome());
        inspection.setConductedDate(request.getConductedDate());
        inspection.setRemarks(request.getRemarks());
        inspection.setGeoCoordinates(request.getGeoCoordinates());
        inspection.setStatus(InspectionStatus.COMPLETED);

        Inspection saved = inspectionRepository.save(inspection);

        // Auto-apply outcome to permit
        permitService.applyInspectionOutcome(inspection.getPermitApplication().getPermitId(), request.getOutcome());

        log.info("Inspection conducted: inspectionId={} outcome={}", inspectionId, request.getOutcome());
        return mapToResponse(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    public List<InspectionResponse> getByPermitId(Long permitId) {
        permitService.getEntityById(permitId); // validate permit exists
        return inspectionRepository.findByPermitApplication_PermitId(permitId)
                .stream().map(this::mapToResponse).toList();
    }

    public List<InspectionResponse> getAssignedToMe(Long officerId) {
        return inspectionRepository.findByAssignedOfficerId(officerId)
                .stream().map(this::mapToResponse).toList();
    }

    public List<InspectionResponse> getScheduledAssignedToMe(Long officerId) {
        return inspectionRepository.findByAssignedOfficerIdAndStatus(officerId, InspectionStatus.SCHEDULED)
                .stream().map(this::mapToResponse).toList();
    }

    public InspectionResponse getById(Long inspectionId) {
        return mapToResponse(getEntityById(inspectionId));
    }

    public List<InspectionResponse> getAll() {
        return inspectionRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<InspectionResponse> getByStatus(InspectionStatus status) {
        return inspectionRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Inspection getEntityById(Long inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with id: " + inspectionId));
    }

    private InspectionResponse mapToResponse(Inspection i) {
        return InspectionResponse.builder()
                .inspectionId(i.getInspectionId())
                .permitId(i.getPermitApplication().getPermitId())
                .assignedOfficerId(i.getAssignedOfficerId())
                .scheduledDate(i.getScheduledDate())
                .conductedDate(i.getConductedDate())
                .outcome(i.getOutcome())
                .remarks(i.getRemarks())
                .status(i.getStatus())
                .geoCoordinates(i.getGeoCoordinates())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
