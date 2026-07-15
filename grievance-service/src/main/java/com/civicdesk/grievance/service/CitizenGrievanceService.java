package com.civicdesk.grievance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.grievance.dto.request.AnalyticsCountDto;
import com.civicdesk.grievance.dto.request.AnalyticsTrendDto;
import com.civicdesk.grievance.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.grievance.dto.request.GrievanceAnalyticsResponse;
import com.civicdesk.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.grievance.dto.response.GrievanceDetailResponse;
import com.civicdesk.grievance.dto.response.GrievanceResponse;
import com.civicdesk.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.entity.GrievanceAction;
import com.civicdesk.grievance.enums.ActionType;
import com.civicdesk.grievance.enums.Category;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
import com.civicdesk.grievance.enums.NotificationType;
import com.civicdesk.grievance.enums.ReferenceType;
import com.civicdesk.grievance.mapper.GrievanceMapper;
import com.civicdesk.grievance.repository.GrievanceActionRepo;
import com.civicdesk.grievance.repository.GrievanceRepo;
import com.civicdesk.grievance.security.JwtUserContext;
import com.civicdesk.grievance.util.SecurityContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.civicdesk.grievance.exception.GrievanceNotFoundException;
import com.civicdesk.grievance.exception.InvalidGrievanceAnalyticsRequestException;
import com.civicdesk.grievance.exception.InvalidGrievanceDataException;
import com.civicdesk.grievance.exception.InvalidGrievanceStateException;
import com.civicdesk.grievance.exception.UnauthorizedGrievanceAccessException;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.dto.response.DepartmentResponse;
import com.civicdesk.grievance.client.AuthClient;
// import com.civicdesk.grievance.util.SecurityContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Citizen-side grievance operations. The caller's id comes from the JWT (via
 * {@link SecurityContextUtil}); a citizen may act only on their own grievances.
 */
@Service
@Slf4j
public class CitizenGrievanceService {

    private ObjectMapper objectMapper;
    private final GrievanceRepo grievanceRepo;
    private final GrievanceActionRepo grievanceActionRepo;
    // private final DepartmentRepository departmentRepository;
    private final AuthClient userClient;
    private final GrievanceMapper mapper;
    // private final AuditLogClient auditLogClient;
    private final AuditHelperService auditHelperService;
    private final NotificationHelperService notificationHelperService;    

    public CitizenGrievanceService(GrievanceRepo grievanceRepo,
                                   GrievanceActionRepo grievanceActionRepo,
                                   AuthClient userClient,
                                   GrievanceMapper mapper,
                                   ObjectMapper objectMapper,
                                   AuditHelperService auditHelperService,
                                   NotificationHelperService notificationHelperService) {
        this.grievanceRepo = grievanceRepo;
        this.grievanceActionRepo = grievanceActionRepo;
        this.userClient = userClient;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.auditHelperService = auditHelperService;
        this.notificationHelperService = notificationHelperService;
    }

    /** Raise a grievance; routes to the category's department and lands with its supervisor (L2). */
    @Transactional
    public GrievanceResponse createGrievance(GrievanceCreateReq req) {
        String citizenId = currentUserId();
        Category category = parseCategory(req.getCategory());

        ApiResponse response =
                userClient.getDepartmentById(category.getDepartmentId());

        DepartmentResponse department =
                objectMapper.convertValue(
                        response.getData(),
                        DepartmentResponse.class
                );

        if (department == null) {
            throw new InvalidGrievanceDataException(
                    "No department is configured for category " + category);
        }

        Grievance grievance = mapper.toEntity(req, category, citizenId,
                department.getDepartmentId(), department.getDepartmentSupervisorId());

        Grievance saved = grievanceRepo.save(grievance);

        notificationHelperService.notify(
                Long.valueOf(department.getDepartmentSupervisorId()),
                "New Grievance Assigned",
                "A new grievance has been submitted and assigned to your department.",
                NotificationType.GRIEVANCE_UPDATE,
                Long.valueOf(saved.getGrievanceId()),
                ReferenceType.GRIEVANCE
        );        

        // createAuditLog("CREATE_GRIEVANCE");
        auditHelperService.log(
                "CREATE_GRIEVANCE"
        );

        return mapper.toResponse(saved);
    }

    /** Edit title/description; allowed only while the grievance is Open and owned by the caller. */
    @Transactional
    public GrievanceResponse updateGrievanceDetails(String grievanceId, GrievanceDetailsUpdateReq req) {
        Grievance grievance = loadOwned(grievanceId);
        if (grievance.getStatus() != GrievanceStatus.O) {
            throw new InvalidGrievanceStateException("A grievance can be edited only while it is Open");
        }
        grievance.setGrievanceTitle(req.getGrievanceTitle());
        grievance.setDescription(req.getDescription());
        // return mapper.toResponse(grievanceRepo.save(grievance));
        Grievance saved = grievanceRepo.save(grievance);

        // createAuditLog("UPDATE_GRIEVANCE");
        auditHelperService.log(
                "UPDATE_GRIEVANCE"
        );        

        return mapper.toResponse(saved);        
    }

    /** The caller's own grievances. */
    @Transactional(readOnly = true)
    public List<GrievanceSummaryResponse> getMyGrievances() {
        log.error("This is for the testing purpose just ignore");
        log.warn("This is for the testing purpose just ignore");
        return grievanceRepo.findByCitizenId(currentUserId())
                .stream().map(mapper::toSummary).toList();
    }

    /** One of the caller's grievances with its full action timeline. */
    @Transactional(readOnly = true)
    public GrievanceDetailResponse getGrievanceById(String grievanceId) {
        Grievance grievance = loadOwned(grievanceId);
        List<GrievanceAction> actions =
                grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc(grievanceId);
        return mapper.toDetail(grievance, actions);
    }

    /** Citizen accepts a resolved grievance -> Closed (terminal). */
    @Transactional
    public GrievanceResponse closeGrievance(String grievanceId) {
        Grievance grievance = loadOwned(grievanceId);
        requireResolved(grievance);
        grievance.setStatus(GrievanceStatus.C);
        // Grievance saved = grievanceRepo.save(grievance);
        // logAction(grievanceId, ActionType.CL, "Grievance closed by citizen", null);
        // return mapper.toResponse(saved);
        Grievance saved = grievanceRepo.save(grievance);

        logAction(
                grievanceId,
                ActionType.CL,
                "Grievance closed by citizen",
                null
        );

        // createAuditLog("CLOSE_GRIEVANCE");
        auditHelperService.log(
                "CLOSE_GRIEVANCE"
        );        

        return mapper.toResponse(saved);        
    }

    /** Citizen rejects a resolved grievance -> Reopened, back to the department supervisor (L2). */
    @Transactional
    public GrievanceResponse reopenGrievance(String grievanceId, GrievanceReopenReq req) {
        Grievance grievance = loadOwned(grievanceId);
        requireResolved(grievance);
        grievance.setStatus(GrievanceStatus.RO);
        grievance.setEscalationLevel(EscalationLevel.L2);
        if (grievance.getDepartmentId() != null) {
            ApiResponse response =
                    userClient.getDepartmentById(grievance.getDepartmentId());

            DepartmentResponse department =
                    objectMapper.convertValue(
                            response.getData(),
                            DepartmentResponse.class
                    );

            if (department != null) {
                grievance.setAssignedToId(department.getDepartmentSupervisorId());
            }
        }
        // Grievance saved = grievanceRepo.save(grievance);
        // logAction(grievanceId, ActionType.RP, "Grievance reopened by citizen", req.getReason());
        // return mapper.toResponse(saved);
        Grievance saved = grievanceRepo.save(grievance);

        if (grievance.getAssignedToId() != null) {

            notificationHelperService.notify(
                    Long.valueOf(grievance.getAssignedToId()),
                    "Grievance Reopened",
                    "A citizen has reopened a grievance that requires your review.",
                    NotificationType.GRIEVANCE_UPDATE,
                    Long.valueOf(saved.getGrievanceId()),
                    ReferenceType.GRIEVANCE
            );
        }        

        logAction(
                grievanceId,
                ActionType.RP,
                "Grievance reopened by citizen",
                req.getReason()
        );

        // createAuditLog("REOPEN_GRIEVANCE");
        auditHelperService.log(
                "REOPEN_GRIEVANCE"
        );        

        return mapper.toResponse(saved);        
    }

    // --- helpers ---

    private Grievance loadOwned(String grievanceId) {
        Grievance grievance = grievanceRepo.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException(
                        "No grievance found with id: " + grievanceId));
        if (!grievance.getCitizenId().equals(currentUserId())) {
            throw new UnauthorizedGrievanceAccessException("You can only access your own grievances");
        }
        return grievance;
    }

    private void requireResolved(Grievance grievance) {
        if (grievance.getStatus() != GrievanceStatus.R) {
            throw new InvalidGrievanceStateException(
                    "Only a Resolved grievance can be closed or reopened");
        }
    }

    private void logAction(String grievanceId, ActionType type, String title, String description) {
        GrievanceAction action = new GrievanceAction();
        action.setGrievanceId(grievanceId);
        action.setTakenById(currentUserId());
        action.setActionType(type);
        action.setGrievanceActionTitle(title);
        action.setActionDescription(description);
        grievanceActionRepo.save(action);
    }

    private Category parseCategory(String value) {
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidGrievanceDataException(
                    "Invalid category '" + value + "'. Valid codes: RI, WS, SN, SD, CR, OT");
        }
    }

    private String currentUserId() {
        return JwtUserContext.getCurrentUserId();
    }

    // private void createAuditLog(String action) {

    //     auditLogClient.createAuditLog(
    //             CreateAuditLogRequest.builder()
    //                     .userId(currentUserId())
    //                     .action(action)
    //                     .module("GRIEVANCE")
    //                     .ipAddress(null)
    //                     .build()
    //     );
    // }

    private <E extends Enum<E>> List<AnalyticsCountDto> fillEnumCounts(List<AnalyticsCountDto> raw, Class<E> enumClass) {
        Map<String, Long> map = new HashMap<>();
        if (raw != null) {
            for (AnalyticsCountDto d : raw) {
                if (d != null) {
                    map.put(d.getLabel(), d.getCount() == null ? 0L : d.getCount());
                }
            }
        }
        List<AnalyticsCountDto> out = new ArrayList<>();
        for (E e : enumClass.getEnumConstants()) {
            String label = e.name();
            if (GrievanceStatus.class.equals(enumClass)) {
                label = formatStatusLabel(label);
            } else if (Category.class.equals(enumClass)) {
                label = formatCategoryLabel(label);
            }
            out.add(new AnalyticsCountDto(label, map.getOrDefault(e.name(), 0L)));
        }
        return out;
    }

    private List<AnalyticsCountDto> fillAssignmentCounts(List<AnalyticsCountDto> raw) {
        Map<String, Long> map = new HashMap<>();
        if (raw != null) {
            for (AnalyticsCountDto d : raw) {
                if (d != null) {
                    map.put(d.getLabel(), d.getCount() == null ? 0L : d.getCount());
                }
            }
        }
        List<AnalyticsCountDto> out = new ArrayList<>();
        out.add(new AnalyticsCountDto(formatAssignmentLabel("UNASSIGNED"), map.getOrDefault("UNASSIGNED", 0L)));
        out.add(new AnalyticsCountDto(formatAssignmentLabel("ASSIGNED"), map.getOrDefault("ASSIGNED", 0L)));
        return out;
    }

    private String formatStatusLabel(String statusCode) {
        return switch (statusCode) {
            case "O" -> "Open";
            case "IP" -> "In Progress";
            case "R" -> "Resolved";
            case "C" -> "Closed";
            case "RO" -> "Reopened";
            default -> statusCode;
        };
    }

    private String formatAssignmentLabel(String assignmentCode) {
        return switch (assignmentCode) {
            case "UNASSIGNED" -> "Unassigned";
            case "ASSIGNED" -> "Assigned";
            default -> assignmentCode;
        };
    }

    private List<AnalyticsTrendDto> fillTrendDates(LocalDate fromDate, LocalDate toDate, List<AnalyticsTrendDto> rawTrend) {
        Map<LocalDate, Long> trendMap = new HashMap<>();
        if (rawTrend != null) {
            for (AnalyticsTrendDto dto : rawTrend) {
                if (dto != null && dto.getDate() != null) {
                    trendMap.put(dto.getDate(), dto.getCount() == null ? 0L : dto.getCount());
                }
            }
        }
        List<AnalyticsTrendDto> trend = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            trend.add(new AnalyticsTrendDto(date, trendMap.getOrDefault(date, 0L)));
        }
        return trend;
    }

    private String formatCategoryLabel(String categoryCode) {
        return switch (categoryCode) {
            case "RI" -> "Road infrastructure";
            case "WS" -> "Water supply";
            case "SN" -> "Sanitation";
            case "SD" -> "Service delay";
            case "CR" -> "Corruption";
            case "OT" -> "Other";
            default -> categoryCode;
        };
    }

    public GrievanceAnalyticsResponse getGrievanceAnalytics(
            GrievanceAnalyticsRequest request) {

        if (request == null) {
            throw new InvalidGrievanceAnalyticsRequestException("Grievance analytics request cannot be null");
        }
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new InvalidGrievanceAnalyticsRequestException("Both fromDate and toDate are required");
        }
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new InvalidGrievanceAnalyticsRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime fromDate = request.getFromDate();
        LocalDateTime toDate = request.getToDate();
        String departmentId = request.getDeptId();

        Long total = grievanceRepo.countGrievances(
                departmentId,
                fromDate,
                toDate
        );

        List<AnalyticsCountDto> rawStatus =
                grievanceRepo.getStatusBreakdown(
                        departmentId,
                        fromDate,
                        toDate
                );

        List<AnalyticsCountDto> rawCategory =
                grievanceRepo.getCategoryBreakdown(
                        departmentId,
                        fromDate,
                        toDate
                );

        List<AnalyticsCountDto> rawEscalation =
                grievanceRepo.getEscalationBreakdown(
                        departmentId,
                        fromDate,
                        toDate
                );

        List<AnalyticsCountDto> rawAssignment =
                grievanceRepo.getAssignmentBreakdown(
                        departmentId,
                        fromDate,
                        toDate
                );

        List<AnalyticsTrendDto> rawTrend =
                grievanceRepo.getTrend(
                        departmentId,
                        fromDate,
                        toDate
                );

        // Ensure all enum values are present with zero counts when missing
        List<AnalyticsCountDto> status = fillEnumCounts(rawStatus, GrievanceStatus.class);
        List<AnalyticsCountDto> category = fillEnumCounts(rawCategory, Category.class);
        List<AnalyticsCountDto> escalation = fillEnumCounts(rawEscalation, EscalationLevel.class);
        List<AnalyticsCountDto> assignment = fillAssignmentCounts(rawAssignment);
        List<AnalyticsTrendDto> trend = fillTrendDates(fromDate.toLocalDate(), toDate.toLocalDate(), rawTrend);

        return GrievanceAnalyticsResponse.builder()
                .totalGrievances(total)
                .statusBreakdown(status)
                .categoryBreakdown(category)
                .escalationBreakdown(escalation)
                .assignmentBreakdown(assignment)
                .trend(trend)
                .build();
    }
}

