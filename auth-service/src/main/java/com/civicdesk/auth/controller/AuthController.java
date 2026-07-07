package com.civicdesk.auth.controller;

import com.civicdesk.auth.dto.request.CitizenLoginRequest;
import com.civicdesk.auth.dto.request.RegisterRequest;
import com.civicdesk.auth.dto.request.SetPasswordRequest;
import com.civicdesk.auth.dto.request.StaffLoginRequest;
import com.civicdesk.auth.dto.response.AuthResponse;
import com.civicdesk.auth.enums.AuditAction;
import com.civicdesk.auth.enums.AuditModule;
import com.civicdesk.auth.response.ApiResponse;
import com.civicdesk.auth.service.AuditService;
import com.civicdesk.auth.service.AuthService;
import com.civicdesk.auth.util.ClientIpUtil;
import com.civicdesk.auth.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/iam/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest httpReq) {
        authService.register(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.status(201).body(ApiResponse.of("Registration successful", null));
    }

    @PostMapping("/citizen/login")
    public ResponseEntity<ApiResponse> citizenLogin(
            @Valid @RequestBody CitizenLoginRequest req,
            HttpServletRequest httpReq) {
        AuthResponse res = authService.citizenLogin(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Login successful", res));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse> staffLogin(
            @Valid @RequestBody StaffLoginRequest req,
            HttpServletRequest httpReq) {
        AuthResponse res = authService.staffLogin(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Login successful", res));
    }

    @PostMapping("/setPassword")
    public ResponseEntity<ApiResponse> setPassword(@Valid @RequestBody SetPasswordRequest req) {
        authService.setPassword(req);
        return ResponseEntity.ok(ApiResponse.of("Password set successfully. Please login.", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest httpReq) {
        String userId = SecurityContextUtil.getCurrentUserId();
        auditService.log(userId, AuditAction.LOGOUT.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Logout successful", null));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse> validateToken(HttpServletRequest httpReq) {
        String token = resolveBearer(httpReq);
        if (!authService.validateToken(token)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Token invalid or expired"));
        }
        return ResponseEntity.ok(ApiResponse.of("Token valid", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse> refreshToken(HttpServletRequest httpReq) {
        String token = resolveBearer(httpReq);
        AuthResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.of("Token refreshed", response));
    }

    @PostMapping("/revoke-token")
    public ResponseEntity<ApiResponse> revokeToken(HttpServletRequest httpReq) {
        String token = resolveBearer(httpReq);
        authService.revokeToken(token);
        return ResponseEntity.ok(ApiResponse.of("Token revoked", null));
    }

    private String resolveBearer(HttpServletRequest httpReq) {
        String header = httpReq.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}
