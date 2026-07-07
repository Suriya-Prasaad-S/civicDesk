package com.civicdesk.grievance.service;

import com.civicdesk.grievance.dto.*;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.entity.GrievanceAction;
import com.civicdesk.grievance.enums.*;
import com.civicdesk.grievance.exception.BadRequestException;
import com.civicdesk.grievance.exception.ForbiddenException;
import com.civicdesk.grievance.exception.ResourceNotFoundException;
import com.civicdesk.grievance.repository.GrievanceActionRepository;
import com.civicdesk.grievance.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceActionRepository actionRepository;

    // SLA days per priority
    private static final Map<GrievancePriority, Integer> SLA_DAYS = Map.of(
            GrievancePriority.CRITICAL, 2,
            GrievancePriority.HIGH,     5,
            GrievancePriority.MEDIUM,  10,
            GrievancePriority.LOW,     15
    );

    // ─── CITIZEN ─────────────────────────────────────────────────────────────

    @Transactional
    public GrievanceResponse submit(GrievanceRequest request, Long userId) {
        GrievancePriority priority = request.getPriority() != null
                ? request.getPriority() : GrievancePriority.MEDIUM;

        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(SLA_DAYS.get(priority));

        Grievance grievance = Grievance.builder()
                .citizenId(request.getCitizenId())
                .userId(userId)
                .category(request.getCategory())
                .subject(request.getSubject())
                .description(request.getDescription())
                .location(request.getLocation())
                .departmentId(request.getDepartmentId())
                .priority(priority)
                .status(GrievanceStatus.SUBMITTED)
                .submittedDate(today)
                .slaDeadline(deadline)
                .slaBreach(false)
                .escalationLevel(EscalationLevel.L1)
                .build();

        Grievance saved = grievanceRepository.save(grievance);

        recordAction(saved, userId, ActionType.SUBMITTED,
                GrievanceStatus.SUBMITTED, GrievanceStatus.SUBMITTED,
                "Grievance submitted by citizen.");

        log.info("Grievance submitted: id={} category={} priority={} slaDeadline={}",
                saved.getGrievanceId(), saved.getCategory(), priority, deadline);
        return mapToResponse(saved);
    }

    @Transactional
    public GrievanceResponse updateDetails(Long grievanceId, GrievanceRequest request, Long userId) {
        Grievance grievance = getEntityById(grievanceId);

        if (!grievance.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own grievances.");
        }

        if (grievance.getStatus() != GrievanceStatus.SUBMITTED) {
            throw new BadRequestException("Grievance can only be edited while it is open.");
        }

        grievance.setSubject(request.getSubject());
        grievance.setDescription(request.getDescription());

        Grievance saved = grievanceRepository.save(grievance);
        log.info("Grievance details updated: id={} userId={}", grievanceId, userId);
        return mapToResponse(saved);
    }

    public List<GrievanceResponse> getMyGrievances(Long userId) {
        return grievanceRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public GrievanceActionResponse addComment(Long grievanceId, String remarks, Long userId, String role) {
        Grievance grievance = getEntityById(grievanceId);

        // Citizens can only comment on their own grievances
        if ("CIT".equals(role) && !grievance.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only comment on your own grievances.");
        }

        GrievanceAction action = recordAction(grievance, userId, ActionType.COMMENT,
                grievance.getStatus(), grievance.getStatus(), remarks);
        return mapActionToResponse(action);
    }

    // ─── SUPERVISOR / ADMIN ───────────────────────────────────────────────────

    @Transactional
    public GrievanceResponse assign(Long grievanceId, AssignGrievanceRequest request, Long assignedBy) {
        Grievance grievance = getEntityById(grievanceId);

        if (grievance.getStatus() == GrievanceStatus.RESOLVED
                || grievance.getStatus() == GrievanceStatus.CLOSED
                || grievance.getStatus() == GrievanceStatus.REJECTED) {
            throw new BadRequestException("Cannot assign a " + grievance.getStatus() + " grievance.");
        }

        GrievanceStatus oldStatus = grievance.getStatus();
        grievance.setAssignedTo(request.getOfficerId());
        grievance.setStatus(GrievanceStatus.ASSIGNED);

        Grievance saved = grievanceRepository.save(grievance);
        recordAction(saved, assignedBy, ActionType.ASSIGNED,
                oldStatus, GrievanceStatus.ASSIGNED,
                request.getRemarks() != null ? request.getRemarks()
                        : "Assigned to officer id=" + request.getOfficerId());

        log.info("Grievance assigned: id={} officerId={}", grievanceId, request.getOfficerId());
        return mapToResponse(saved);
    }

    @Transactional
    public GrievanceResponse escalate(Long grievanceId, EscalateRequest request, Long userId) {
        Grievance grievance = getEntityById(grievanceId);

        EscalationLevel current = grievance.getEscalationLevel();
        EscalationLevel next    = request.getEscalationLevel();

        // Must be a forward escalation
        if (next.ordinal() <= current.ordinal()) {
            throw new BadRequestException(
                    "Escalation level must move forward. Current: " + current + ", Requested: " + next);
        }

        GrievanceStatus oldStatus = grievance.getStatus();
        grievance.setEscalationLevel(next);
        grievance.setStatus(GrievanceStatus.ESCALATED);

        Grievance saved = grievanceRepository.save(grievance);
        recordAction(saved, userId, ActionType.ESCALATED,
                oldStatus, GrievanceStatus.ESCALATED,
                request.getRemarks() != null ? request.getRemarks()
                        : "Escalated from " + current + " to " + next);

        log.info("Grievance escalated: id={} from={} to={}", grievanceId, current, next);
        return mapToResponse(saved);
    }

    // ─── FIELD OFFICER / SUPERVISOR / ADMIN ──────────────────────────────────

    @Transactional
    public GrievanceResponse updateStatus(Long grievanceId, UpdateStatusRequest request,
                                           Long userId, String role) {
        Grievance grievance = getEntityById(grievanceId);

        validateStatusTransition(grievance.getStatus(), request.getStatus(), role);

        // Field officers may only update their assigned grievances
        if ("FO".equals(role) && !userId.equals(grievance.getAssignedTo())) {
            throw new ForbiddenException("You can only update grievances assigned to you.");
        }

        GrievanceStatus oldStatus = grievance.getStatus();
        grievance.setStatus(request.getStatus());

        if (request.getStatus() == GrievanceStatus.RESOLVED) {
            grievance.setResolvedDate(LocalDate.now());
        }

        Grievance saved = grievanceRepository.save(grievance);
        recordAction(saved, userId, ActionType.STATUS_UPDATED,
                oldStatus, request.getStatus(), request.getRemarks());

        log.info("Grievance status updated: id={} {} → {}", grievanceId, oldStatus, request.getStatus());
        return mapToResponse(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    public GrievanceResponse getById(Long grievanceId, Long userId, String role) {
        Grievance grievance = getEntityById(grievanceId);
        if ("CIT".equals(role) && !grievance.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only view your own grievances.");
        }
        return mapToResponse(grievance);
    }

    public List<GrievanceResponse> getAll() {
        return grievanceRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceResponse> getByStatus(GrievanceStatus status) {
        return grievanceRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceResponse> getByDepartment(Long departmentId) {
        return grievanceRepository.findByDepartmentId(departmentId).stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceResponse> getAssignedToMe(Long officerId) {
        return grievanceRepository.findByAssignedTo(officerId).stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceResponse> getByEscalationLevel(EscalationLevel level) {
        return grievanceRepository.findByEscalationLevel(level).stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceResponse> getSlaBreached() {
        return grievanceRepository.findSlaBreached(LocalDate.now())
                .stream().map(this::mapToResponse).toList();
    }

    public List<GrievanceActionResponse> getTimeline(Long grievanceId) {
        getEntityById(grievanceId); // validate exists
        return actionRepository.findByGrievance_GrievanceIdOrderByCreatedAtAsc(grievanceId)
                .stream().map(this::mapActionToResponse).toList();
    }

    // ─── SLA BREACH SCHEDULER ─────────────────────────────────────────────────

    /**
     * Runs daily at midnight.
     * Marks overdue grievances as SLA-breached and auto-escalates them one level.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processSlaBreaches() {
        List<Grievance> breached = grievanceRepository.findSlaBreached(LocalDate.now());
        log.info("SLA breach check: {} grievances overdue", breached.size());

        for (Grievance g : breached) {
            g.setSlaBreach(true);

            // Auto-escalate if not already at L3
            EscalationLevel next = switch (g.getEscalationLevel()) {
                case L1 -> EscalationLevel.L2;
                case L2 -> EscalationLevel.L3;
                case L3 -> null; // already max
            };

            GrievanceStatus oldStatus = g.getStatus();
            if (next != null) {
                g.setEscalationLevel(next);
                g.setStatus(GrievanceStatus.ESCALATED);
                recordAction(g, 0L, ActionType.ESCALATED,
                        oldStatus, GrievanceStatus.ESCALATED,
                        "Auto-escalated due to SLA breach. Escalated to " + next);
                log.warn("Auto-escalated grievanceId={} to {}", g.getGrievanceId(), next);
            } else {
                recordAction(g, 0L, ActionType.STATUS_UPDATED,
                        oldStatus, oldStatus,
                        "SLA breach recorded. Already at maximum escalation level L3.");
            }

            grievanceRepository.save(g);
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void validateStatusTransition(GrievanceStatus current, GrievanceStatus next, String role) {
        boolean valid = switch (current) {
            case SUBMITTED -> next == GrievanceStatus.ASSIGNED || next == GrievanceStatus.REJECTED;
            case ASSIGNED  -> next == GrievanceStatus.IN_PROGRESS || next == GrievanceStatus.REJECTED;
            case IN_PROGRESS -> next == GrievanceStatus.RESOLVED || next == GrievanceStatus.ESCALATED;
            case ESCALATED   -> next == GrievanceStatus.IN_PROGRESS || next == GrievanceStatus.RESOLVED
                             || next == GrievanceStatus.REJECTED;
            case RESOLVED    -> next == GrievanceStatus.CLOSED;
            case CLOSED, REJECTED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " → " + next);
        }

        // Field officers cannot reject or close
        if ("FO".equals(role)
                && (next == GrievanceStatus.REJECTED || next == GrievanceStatus.CLOSED)) {
            throw new ForbiddenException("Only supervisors or admins can reject or close grievances.");
        }
    }

    private GrievanceAction recordAction(Grievance grievance, Long userId,
                                          ActionType type, GrievanceStatus oldStatus,
                                          GrievanceStatus newStatus, String remarks) {
        GrievanceAction action = GrievanceAction.builder()
                .grievance(grievance)
                .actionTakenBy(userId)
                .actionType(type)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .remarks(remarks)
                .build();
        return actionRepository.save(action);
    }

    public Grievance getEntityById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with id: " + id));
    }

    private GrievanceResponse mapToResponse(Grievance g) {
        return GrievanceResponse.builder()
                .grievanceId(g.getGrievanceId())
                .citizenId(g.getCitizenId())
                .userId(g.getUserId())
                .category(g.getCategory())
                .subject(g.getSubject())
                .description(g.getDescription())
                .location(g.getLocation())
                .departmentId(g.getDepartmentId())
                .priority(g.getPriority())
                .status(g.getStatus())
                .assignedTo(g.getAssignedTo())
                .submittedDate(g.getSubmittedDate())
                .resolvedDate(g.getResolvedDate())
                .slaDeadline(g.getSlaDeadline())
                .slaBreach(g.getSlaBreach())
                .escalationLevel(g.getEscalationLevel())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private GrievanceActionResponse mapActionToResponse(GrievanceAction a) {
        return GrievanceActionResponse.builder()
                .actionId(a.getActionId())
                .grievanceId(a.getGrievance().getGrievanceId())
                .actionTakenBy(a.getActionTakenBy())
                .actionType(a.getActionType())
                .remarks(a.getRemarks())
                .oldStatus(a.getOldStatus())
                .newStatus(a.getNewStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
