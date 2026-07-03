package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.CitizenStatus;
import com.civicdesk.citizen.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CitizenProfileResponse {
    private Long citizenId;
    private Long userId;
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String nationalIdNumber;
    private String address;
    private String ward;
    private String zone;
    private String email;
    private String phone;
    private CitizenStatus status;
    private LocalDateTime createdAt;
}
