package com.civicdesk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordRequest {

    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "newPassword is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
