package com.civicdesk.grievance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.grievance.dto.request.ResolveReq;
import com.civicdesk.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.entity.GrievanceAction;
import com.civicdesk.grievance.enums.ActionType;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
import com.civicdesk.grievance.enums.NotificationType;
import com.civicdesk.grievance.enums.ReferenceType;
import com.civicdesk.grievance.exception.GrievanceNotFoundException;
import com.civicdesk.grievance.exception.InvalidGrievanceDataException;
import com.civicdesk.grievance.exception.InvalidGrievanceStateException;
import com.civicdesk.grievance.exception.UnauthorizedGrievanceAccessException;
import com.civicdesk.grievance.mapper.GrievanceMapper;
import com.civicdesk.grievance.repository.GrievanceActionRepo;
import com.civicdesk.grievance.repository.GrievanceRepo;
import com.civicdesk.grievance.security.JwtUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.dto.response.UserResponse;
import com.civicdesk.grievance.enums.Role;
import com.civicdesk.grievance.enums.UserStatus;


import lombok.extern.slf4j.Slf4j;

/**
 * Department-supervisor (DS) grievance operations: view the department queue,
 * assign/reassign a field officer, resolve, and view one grievance. A supervisor
 * may act only within their own department. User identity/listing belongs to IAM;
 * here we only read users by id (via {@link UserRepository}) to resolve the
 * supervisor's department and to validate a chosen field officer.
 */
@Service
@Slf4j
public class SupervisorGrievanceService {

    private final GrievanceRepo grievanceRepo;
    private final GrievanceActionRepo grievanceActionRepo;
    private final GrievanceMapper mapper;
    private final AuthClient userClient;
    private ObjectMapper objectMapper;
    private final AuditHelperService auditHelperService;
    private final NotificationHelperService notificationHelperService;

    public SupervisorGrievanceService(GrievanceRepo grievanceRepo,
                                      GrievanceActionRepo grievanceActionRepo,
                                      GrievanceMapper mapper,
                                      AuthClient userClient,
                                      ObjectMapper objectMapper,
                                      AuditHelperService auditHelperService,
                                      NotificationHelperService notificationHelperService) {
        this.grievanceRepo = grievanceRepo;
        this.grievanceActionRepo = grievanceActionRepo;
        this.mapper = mapper;
        this.userClient = userClient;
        this.objectMapper = objectMapper;
        this.auditHelperService = auditHelperService;
        this.notificationHelperService = notificationHelperService;
    }
    

    private UserResponse getUserResponse(ApiResponse resp) {
        if (resp == null || resp.getData() == null) {
            return null;
        }

        return objectMapper.convertValue(
                resp.getData(),
                UserResponse.class
        );
    }

    /** Grievances in the supervisor's department. */
    @Transactional(readOnly = true)
    public List<GrievanceSummaryResponse> getDepartmentGrievances() {
        String deptId = supervisorDepartmentId();
        log.info("Testing the logging...");
        return grievanceRepo.findByDepartmentId(deptId)
                .stream().map(mapper::toSummary).toList();
    }

    /** Assign (or reassign) a field officer to a grievance in the supervisor's department. */
    @Transactional
    public GrievanceSummaryResponse assignFieldOfficer(String grievanceId, AssignFieldOfficerReq req) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        requireWithSupervisor(grievance);
        validateFieldOfficer(req.getFieldOfficerId(), deptId);

        grievance.setFieldOfficerId(req.getFieldOfficerId());
        grievance.setAssignedToId(req.getFieldOfficerId());
        grievance.setEscalationLevel(EscalationLevel.L1);
        grievance.setStatus(GrievanceStatus.IP);
        Grievance saved = grievanceRepo.save(grievance);

        logAction(grievanceId, ActionType.AS, "Assigned to field officer", req.getMessage());

        notificationHelperService.notify(
                Long.valueOf(req.getFieldOfficerId()),
                "Grievance Assigned",
                "A grievance has been assigned to you for action.",
                NotificationType.GRIEVANCE_UPDATE,
                Long.valueOf(saved.getGrievanceId()),
                ReferenceType.GRIEVANCE
        );        

        auditHelperService.log("ASSIGN_FIELD_OFFICER");
        
        return mapper.toSummary(saved);
    }

    /** Resolve a grievance (with a message); it then goes to the citizen for close/reopen. */
    @Transactional
    public GrievanceSummaryResponse resolveGrievance(String grievanceId, ResolveReq req) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        requireWithSupervisor(grievance);

        grievance.setStatus(GrievanceStatus.R);
        Grievance saved = grievanceRepo.save(grievance);

        logAction(grievanceId, ActionType.RS, "Grievance resolved by supervisor", req.getMessage());

        notificationHelperService.notify(
                Long.valueOf(grievance.getCitizenId()),
                "Grievance Resolved",
                "Your grievance has been resolved. Please review and close or reopen it.",
                NotificationType.GRIEVANCE_UPDATE,
                Long.valueOf(saved.getGrievanceId()),
                ReferenceType.GRIEVANCE
        );

        auditHelperService.log("RESOLVE_GRIEVANCE");
        return mapper.toSummary(saved);
    }

    /** One grievance in the supervisor's department, with its timeline. */
    @Transactional(readOnly = true)
    public GrievanceDetailResponse viewDepartmentGrievance(String grievanceId) {
        String deptId = supervisorDepartmentId();
        Grievance grievance = loadInDept(grievanceId, deptId);
        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId);
        return mapper.toDetail(grievance, actions);
    }

    // --- helpers ---

    /** The supervisor's department, resolved from their user record. */
    private String supervisorDepartmentId() {
        String userId = JwtUserContext.getCurrentUserId();
        ApiResponse resp = userClient.getUserById(userId);

        UserResponse supervisor = getUserResponse(resp);
        if (supervisor == null) {
            throw new UnauthorizedGrievanceAccessException("Your account could not be found");
        }
        String deptId = supervisor.getDepartmentId();
        if (deptId == null || deptId.isBlank()) {
            throw new UnauthorizedGrievanceAccessException(
                    "Your account is not assigned to a department");
        }
        return deptId;
    }

    /** Load a grievance and confirm it belongs to the supervisor's department. */
    private Grievance loadInDept(String grievanceId, String deptId) {
        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));
        if (!deptId.equals(grievance.getDepartmentId())) {
            throw new UnauthorizedGrievanceAccessException(
                    "This grievance does not belong to your department");
        }
        return grievance;
    }

    /** Assign/resolve are allowed only while the grievance is with the supervisor (L2, not resolved/closed). */
    private void requireWithSupervisor(Grievance grievance) {
        boolean withSupervisor = grievance.getEscalationLevel() == EscalationLevel.L2;
        boolean actionable = grievance.getStatus() != GrievanceStatus.R
                && grievance.getStatus() != GrievanceStatus.C;
        if (!withSupervisor || !actionable) {
            throw new InvalidGrievanceStateException(
                    "This grievance is not currently with the supervisor "
                            + "(it may be with a field officer, resolved, or closed)");
        }
    }

    /** The chosen field officer must be an active FO in the supervisor's department. */
    private void validateFieldOfficer(String fieldOfficerId, String deptId) {
        ApiResponse officerResp = userClient.getUserById(fieldOfficerId);
        UserResponse officer = getUserResponse(officerResp);

        if (officer == null) {
            throw new InvalidGrievanceDataException("No user found with id: " + fieldOfficerId);
        }
        if (!Role.FO.name().equals(officer.getRole())) {
            throw new InvalidGrievanceDataException("The selected user is not a field officer");
        }
        if (!deptId.equals(officer.getDepartmentId())) {
            throw new InvalidGrievanceDataException(
                    "The selected field officer is not in your department");
        }
        if (!UserStatus.ACT.getLabel().equals(officer.getStatus())) {
            throw new InvalidGrievanceDataException("The selected field officer is not active");
        }
    }

    private void logAction(String grievanceId, ActionType type, String title, String message) {
        GrievanceAction action = new GrievanceAction();
        action.setGrievanceId(grievanceId);
        action.setTakenById(JwtUserContext.getCurrentUserId());
        action.setActionType(type);
        action.setGrievanceActionTitle(title);
        action.setActionDescription(message);
        grievanceActionRepo.save(action);
    }
}
