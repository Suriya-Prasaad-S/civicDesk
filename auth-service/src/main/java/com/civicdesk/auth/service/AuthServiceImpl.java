package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.request.CitizenLoginRequest;
import com.civicdesk.auth.dto.request.RegisterRequest;
import com.civicdesk.auth.dto.request.SetPasswordRequest;
import com.civicdesk.auth.dto.request.StaffLoginRequest;
import com.civicdesk.auth.dto.response.AuthResponse;
import com.civicdesk.auth.entity.User;
import com.civicdesk.auth.enums.AuditAction;
import com.civicdesk.auth.enums.AuditModule;
import com.civicdesk.auth.enums.Role;
import com.civicdesk.auth.enums.UserStatus;
import com.civicdesk.auth.exception.AccountInactiveException;
import com.civicdesk.auth.exception.AccountSuspendedException;
import com.civicdesk.auth.exception.BadCredentialsException;
import com.civicdesk.auth.exception.BadRequestException;
import com.civicdesk.auth.exception.DuplicateEmailException;
import com.civicdesk.auth.exception.ForbiddenException;
import com.civicdesk.auth.exception.PasswordNotSetException;
import com.civicdesk.auth.exception.ResourceNotFoundException;
import com.civicdesk.auth.entity.PasswordResetToken;
import com.civicdesk.auth.entity.RevokedToken;
import com.civicdesk.auth.repository.PasswordResetTokenRepository;
import com.civicdesk.auth.repository.RevokedTokenRepository;
import com.civicdesk.auth.repository.UserRepository;
import com.civicdesk.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.jwt.expiry}")
    private long expiry;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuditService auditService,
                           RevokedTokenRepository revokedTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    @Transactional
    public void register(RegisterRequest req, String ip) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPasswordSet(true);
        user.setPhone(req.getPhone());
        user.setRole(Role.CIT.name());
        user.setStatus(UserStatus.ACT.getLabel());
        userRepository.save(user);

        auditService.log(user.getUserId(), AuditAction.REGISTER.name(), AuditModule.IAM.name(), ip);
    }

    @Override
    public AuthResponse citizenLogin(CitizenLoginRequest req, String ip) {
        User user = authenticate(req.getEmail(), req.getPassword());
        if (!user.getRole().equals(Role.CIT.name())) {
            throw new ForbiddenException("Please use the staff portal");
        }
        return issueToken(user, ip);
    }

    @Override
    public AuthResponse staffLogin(StaffLoginRequest req, String ip) {
        User user = authenticate(req.getEmail(), req.getPassword());
        if (user.getRole().equals(Role.CIT.name())) {
            throw new ForbiddenException("Please use the citizen portal");
        }
        return issueToken(user, ip);
    }

    private User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isPasswordSet()) {
            throw new PasswordNotSetException("Please set your password before logging in");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String status = UserStatus.normalize(user.getStatus());
        if (UserStatus.SUS.getLabel().equals(status)) {
            throw new AccountSuspendedException("Account suspended contact admin");
        }
        if (!UserStatus.ACT.getLabel().equals(status)) {
            throw new AccountInactiveException("Your account is inactive. Please contact your administrator to reactivate it.");
        }
        return user;
    }

    @Override
    @Transactional
    public void setPassword(SetPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasResetToken = req.getResetToken() != null && !req.getResetToken().isBlank();
        if (!hasResetToken && user.isPasswordSet()) {
            throw new ForbiddenException("Password already set. Use forgot password to reset.");
        }

        if (req.getNewPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }

        if (hasResetToken) {
            PasswordResetToken resetToken = passwordResetTokenRepository.findById(req.getResetToken())
                    .orElseThrow(() -> new BadRequestException("Invalid reset token"));
            if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                passwordResetTokenRepository.delete(resetToken);
                throw new BadRequestException("Reset token has expired");
            }
            if (!resetToken.getUserId().equals(user.getUserId())) {
                throw new BadRequestException("Invalid reset token for this user");
            }
            passwordResetTokenRepository.delete(resetToken);
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordSet(true);
        userRepository.save(user);

        auditService.log(user.getUserId(), AuditAction.SET_PASSWORD.name(), AuditModule.IAM.name(), "SYSTEM");
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        return !revokedTokenRepository.existsById(token);
    }

    @Override
    public AuthResponse refreshToken(String token) {
        if (!validateToken(token)) {
            throw new BadRequestException("Invalid or expired token");
        }
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        String freshToken = jwtUtil.generateToken(userId, role);
        return new AuthResponse(freshToken, userId, role, expiry / 1000);
    }

    @Override
    public void revokeToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BadRequestException("Invalid token");
        }
        if (revokedTokenRepository.existsById(token)) {
            return;
        }
        RevokedToken revokedToken = new RevokedToken(token, LocalDateTime.ofInstant(jwtUtil.extractExpiration(token).toInstant(), java.time.ZoneId.systemDefault()));
        revokedTokenRepository.save(revokedToken);
    }

    @Override
    public String forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(resetToken, user.getUserId(), LocalDateTime.now().plusHours(2));
        passwordResetTokenRepository.save(token);

        auditService.log(user.getUserId(), AuditAction.SET_PASSWORD.name(), AuditModule.IAM.name(), "SYSTEM");
        return resetToken;
    }

    private AuthResponse issueToken(User user, String ip) {
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());
        auditService.log(user.getUserId(), AuditAction.LOGIN.name(), AuditModule.IAM.name(), ip);
        return new AuthResponse(token, user.getUserId(), user.getRole(), expiry / 1000);
    }
}
