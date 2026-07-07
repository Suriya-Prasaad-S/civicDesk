package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.request.CitizenLoginRequest;
import com.civicdesk.auth.dto.request.RegisterRequest;
import com.civicdesk.auth.dto.request.SetPasswordRequest;
import com.civicdesk.auth.dto.request.StaffLoginRequest;
import com.civicdesk.auth.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest req, String ip);

    AuthResponse citizenLogin(CitizenLoginRequest req, String ip);

    AuthResponse staffLogin(StaffLoginRequest req, String ip);

    void setPassword(SetPasswordRequest req);

    boolean validateToken(String token);

    AuthResponse refreshToken(String token);

    void revokeToken(String token);

    String forgotPassword(String email);
}
