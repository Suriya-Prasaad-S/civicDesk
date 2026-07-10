package com.civicdesk.grievance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicdesk.grievance.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.service.CitizenGrievanceService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/** Citizen-facing grievance endpoints. All require the {@code CIT} role. */
@RestController
@RequestMapping("/grievance")
@PreAuthorize("hasRole('CIT')")
@Slf4j
public class CitizenGrievanceController {

    private final CitizenGrievanceService citizenGrievanceService;

    public CitizenGrievanceController(CitizenGrievanceService citizenGrievanceService) {
        this.citizenGrievanceService = citizenGrievanceService;
    }

    @PostMapping("createGrievance") //done
    public ResponseEntity<ApiResponse> createGrievance(@Valid @RequestBody GrievanceCreateReq req) {
        log.info("This is the request {}", req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Grievance created successfully", citizenGrievanceService.createGrievance(req)));
    }

    @PutMapping("updateGrievanceDetails/{grievanceId}")
    public ResponseEntity<ApiResponse> updateGrievanceDetails(
            @PathVariable String grievanceId, @Valid @RequestBody GrievanceDetailsUpdateReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance updated successfully", citizenGrievanceService.updateGrievanceDetails(grievanceId, req)));
    }

    @GetMapping("getMyGrievances") //done
    public ResponseEntity<ApiResponse> getMyGrievances() {
        return ResponseEntity.ok(ApiResponse.data(citizenGrievanceService.getMyGrievances()));
    }

    @GetMapping("getGrievanceById/{grievanceId}") //done
    public ResponseEntity<ApiResponse> getGrievanceById(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.data(citizenGrievanceService.getGrievanceById(grievanceId)));
    }

    @PostMapping("closeGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> closeGrievance(@PathVariable String grievanceId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance closed successfully", citizenGrievanceService.closeGrievance(grievanceId)));
    }

    @PostMapping("reopenGrievance/{grievanceId}")
    public ResponseEntity<ApiResponse> reopenGrievance(
            @PathVariable String grievanceId, @Valid @RequestBody GrievanceReopenReq req) {
        return ResponseEntity.ok(ApiResponse.of(
                "Grievance reopened successfully", citizenGrievanceService.reopenGrievance(grievanceId, req)));
    }



}
