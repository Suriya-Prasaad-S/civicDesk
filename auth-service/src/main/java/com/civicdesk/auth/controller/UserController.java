package com.civicdesk.auth.controller;

import com.civicdesk.auth.dto.request.CreateUserRequest;
import com.civicdesk.auth.dto.request.UpdateUserRequest;
import com.civicdesk.auth.dto.request.UpdateUserStatusRequest;
import com.civicdesk.auth.dto.response.UserResponse;
import com.civicdesk.auth.enums.AuditAction;
import com.civicdesk.auth.enums.AuditModule;
import com.civicdesk.auth.response.ApiResponse;
import com.civicdesk.auth.response.PageResponse;
import com.civicdesk.auth.service.AuditService;
import com.civicdesk.auth.service.UserService;
import com.civicdesk.auth.util.ClientIpUtil;
import com.civicdesk.auth.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam/users")
public class UserController {

    private final UserService userService;
    private final AuditService auditService;

    public UserController(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMe() {
        String userId = SecurityContextUtil.getCurrentUserId();
        UserResponse user = userService.getById(userId);
        return ResponseEntity.ok(ApiResponse.data(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> createUser(
            @Valid @RequestBody CreateUserRequest req,
            HttpServletRequest httpReq) {
        String callerRole = SecurityContextUtil.getCurrentRole();
        String callerUserId = SecurityContextUtil.getCurrentUserId();
        userService.createUser(req, callerRole, callerUserId);
        auditService.log(callerUserId, AuditAction.CREATE_USER.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.status(201).body(ApiResponse.of("User created successfully", null));
    }

    // NOTE: Commented out to resolve an ambiguous mapping with
    // UserLookupController#getUserById (@GetMapping("/{userId}")). Both mapped
    // GET /iam/users/{singleSegment}, so any such request threw at runtime.
    // The UserLookupController version (returns a bare UserDto) is the one used
    // by inter-service calls (e.g. citizen-service). Re-enable with a distinct
    // path if the ApiResponse-wrapped variant is needed by another client.
    // @GetMapping("/{id}")
    // public ResponseEntity<ApiResponse> getUserById(@PathVariable String id) {
    //     UserResponse user = userService.getById(id);
    //     return ResponseEntity.ok(ApiResponse.data(user));
    // }

    @GetMapping("/{id}/roles")
    public ResponseEntity<ApiResponse> getUserRoles(@PathVariable String id) {
        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.data(java.util.List.of(user.getRole())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable String id,
                                                  @Valid @RequestBody UpdateUserRequest req,
                                                  HttpServletRequest httpReq) {
        String callerRole = SecurityContextUtil.getCurrentRole();
        String callerUserId = SecurityContextUtil.getCurrentUserId();
        UserResponse updated = userService.updateUser(id, req, callerRole, callerUserId);
        auditService.log(callerUserId, AuditAction.UPDATE_USER.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("User updated successfully", updated));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String callerRole = SecurityContextUtil.getCurrentRole();
        String callerUserId = SecurityContextUtil.getCurrentUserId();
        PageResponse<UserResponse> users = userService.getUsers(callerRole, callerUserId, role, status, departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.data(users));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADM')")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest req,
            HttpServletRequest httpReq) {
        String adminId = SecurityContextUtil.getCurrentUserId();
        userService.updateStatus(id, req.getStatus());
        auditService.log(adminId, AuditAction.UPDATE_STATUS.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("User status updated to " + req.getStatus(), null));
    }
}
