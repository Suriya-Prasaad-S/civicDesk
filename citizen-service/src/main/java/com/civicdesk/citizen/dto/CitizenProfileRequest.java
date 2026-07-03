package com.civicdesk.citizen.dto;

import com.civicdesk.citizen.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CitizenProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "National ID number is required")
    private String nationalIdNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "Zone is required")
    private String zone;

    private String phone;
}
