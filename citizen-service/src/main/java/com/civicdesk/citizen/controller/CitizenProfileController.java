package com.civicdesk.citizen.controller;

import com.civicdesk.citizen.dto.ApiResponse;
import com.civicdesk.citizen.dto.CitizenProfileRequest;
import com.civicdesk.citizen.dto.CitizenProfileResponse;
import com.civicdesk.citizen.dto.UpdateCitizenStatusRequest;
import com.civicdesk.citizen.security.JwtUserContext;
import com.civicdesk.citizen.service.CitizenProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/civicDesk/citizens")
@RequiredArgsConstructor
@Tag(name = "Citizen Profile", description = "Citizen Profile management — create, view, update, status control")
@SecurityRequirement(name = "BearerAuth")
public class CitizenProfileController {

    private final CitizenProfileService profileService;

    // ─── CITIZEN ENDPOINTS ────────────────────────────────────────────────────

    @PostMapping("/registerCitizen")
    @PreAuthorize("hasAuthority('ROLE_CIT')")
    @Operation(summary = "Register citizen profile", description = "ROLE_CIT. One-time profile setup after registration.")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> createProfile(
            @Valid @RequestBody CitizenProfileRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String email = JwtUserContext.getCurrentEmail();
        CitizenProfileResponse profile = profileService.createProfile(request, userId, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CitizenProfileResponse>builder()
                        .success(true).message("Citizen profile created successfully").data(profile).build());
    }

    @GetMapping("/getProfile/{citizenId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS','ROLE_FO','ROLE_CO')")
    @Operation(summary = "Get citizen profile by ID")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> getCitizenById(@PathVariable Long citizenId) {
        return ResponseEntity.ok(ApiResponse.<CitizenProfileResponse>builder()
                .success(true).message("Citizen fetched successfully").data(profileService.getById(citizenId)).build());
    }

    @PutMapping("/updateProfile/{citizenId}")
    @PreAuthorize("hasAuthority('ROLE_CIT')")
    @Operation(summary = "Update citizen profile")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> updateProfile(
            @PathVariable Long citizenId,
            @Valid @RequestBody CitizenProfileRequest request) {
        CitizenProfileResponse profile = profileService.updateProfile(request, JwtUserContext.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.<CitizenProfileResponse>builder()
                .success(true).message("Profile updated successfully").data(profile).build());
    }

    @PutMapping("/updateStatus/{citizenId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS')")
    @Operation(summary = "Update citizen status (ACTIVE/VERIFIED/FLAGGED)")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> updateStatus(
            @PathVariable Long citizenId,
            @Valid @RequestBody UpdateCitizenStatusRequest request) {
        CitizenProfileResponse updated = profileService.updateStatus(citizenId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.<CitizenProfileResponse>builder()
                .success(true).message("Citizen status updated to " + request.getStatus()).data(updated).build());
    }

    @GetMapping("/getCitizensByWard/{ward}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Get citizens by ward")
    public ResponseEntity<ApiResponse<List<CitizenProfileResponse>>> getByWard(@PathVariable String ward) {
        return ResponseEntity.ok(ApiResponse.<List<CitizenProfileResponse>>builder()
                .success(true).message("Citizens in ward: " + ward).data(profileService.getByWard(ward)).build());
    }

    @GetMapping("/getAllCitizens")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @Operation(summary = "Get all citizens")
    public ResponseEntity<ApiResponse<List<CitizenProfileResponse>>> getAllCitizens() {
        return ResponseEntity.ok(ApiResponse.<List<CitizenProfileResponse>>builder()
                .success(true).message("Citizens fetched successfully").data(profileService.getAll()).build());
    }
}
