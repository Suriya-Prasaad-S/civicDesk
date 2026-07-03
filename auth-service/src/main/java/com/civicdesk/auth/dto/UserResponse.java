package com.civicdesk.auth.dto;

import com.civicdesk.auth.enums.Role;
import com.civicdesk.auth.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String phone;
    private Long departmentId;
    private UserStatus status;
    private LocalDateTime createdAt;
}
