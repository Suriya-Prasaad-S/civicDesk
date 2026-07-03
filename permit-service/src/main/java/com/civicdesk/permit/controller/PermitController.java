package com.civicdesk.permit.controller;

import com.civicdesk.permit.dto.ApiResponse;
import com.civicdesk.permit.dto.DocumentResponse;
import com.civicdesk.permit.dto.PermitApplicationRequest;
import com.civicdesk.permit.dto.PermitApplicationResponse;
import com.civicdesk.permit.dto.RenewPermitRequest;
import com.civicdesk.permit.dto.UpdatePermitStatusRequest;
import com.civicdesk.permit.dto.VerifyDocumentRequest;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import com.civicdesk.permit.security.JwtUserContext;
import com.civicdesk.permit.service.PermitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/civicDesk/permits")
@RequiredArgsConstructor
@Tag(name = "Permit Management", description = "Building permits, trade licenses, event permissions, advertisement licenses")
@SecurityRequirement(name = "BearerAuth")
public class PermitController {

    private final PermitService permitService;

    @PostMapping("/createPermit")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Apply for a permit")
    public ResponseEntity<ApiResponse<PermitApplicationResponse>> apply(
            @Valid @RequestBody PermitApplicationRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        PermitApplicationResponse response = permitService.applyForPermit(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PermitApplicationResponse>builder()
                .success(true).message("Permit application created successfully").data(response).build());
    }

    @GetMapping("/getAllPermits")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','CO','ADM')")
    @Operation(summary = "List permits — Citizen sees own, Staff sees all")
    public ResponseEntity<ApiResponse<List<PermitApplicationResponse>>> getAll(
            @RequestParam(required = false) PermitStatus status,
            @RequestParam(required = false) PermitType type) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        List<PermitApplicationResponse> data;
        if (role != null && role.contains("CIT")) {
            data = permitService.getMyPermits(userId);
        } else if (status != null) {
            data = permitService.getByStatus(status);
        } else if (type != null) {
            data = permitService.getByType(type);
        } else {
            data = permitService.getAll();
        }
        return ResponseEntity.ok(ApiResponse.<List<PermitApplicationResponse>>builder()
                .success(true).message("Permits fetched successfully").data(data).build());
    }

    @GetMapping("/{permitId}")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','CO','ADM')")
    @Operation(summary = "Get permit by ID")
    public ResponseEntity<ApiResponse<PermitApplicationResponse>> getById(@PathVariable Long permitId) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        PermitApplicationResponse response = permitService.getById(permitId, userId, role);
        return ResponseEntity.ok(ApiResponse.<PermitApplicationResponse>builder()
                .success(true).message("Permit details fetched successfully").data(response).build());
    }

    // POST /{permitId}/uploadDocuments  (multipart/form-data, supports multiple files)
    @PostMapping("/{permitId}/uploadDocuments")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Upload permit document(s) — send documentType & file as lists for multiple files")
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @PathVariable Long permitId,
            @RequestParam("documentType") List<String> documentTypes,
            @RequestParam("file") List<MultipartFile> files) {
        if (documentTypes.size() != files.size()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false).message("Number of documentType values must match number of files").build());
        }
        permitService.uploadDocuments(permitId, documentTypes, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Void>builder()
                .success(true).message("Document(s) uploaded successfully").build());
    }

    // GET /{permitId}/documents
    @GetMapping("/{permitId}/documents")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','CO','ADM')")
    @Operation(summary = "Get permit documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable Long permitId) {
        List<DocumentResponse> docs = permitService.getDocuments(permitId);
        return ResponseEntity.ok(ApiResponse.<List<DocumentResponse>>builder()
                .success(true).message("Documents fetched successfully").data(docs).build());
    }

    // PUT /{permitId}/documents/{documentId}/verify   (documentId is a UUID string)
    @PutMapping("/{permitId}/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Verify a permit document")
    public ResponseEntity<ApiResponse<Void>> verifyDocument(
            @PathVariable Long permitId,
            @PathVariable String documentId,
            @RequestBody VerifyDocumentRequest request) {
        permitService.verifyDocument(permitId, documentId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Document verification status updated successfully").build());
    }

    // GET /documents/{documentId}/download
    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a permit document file")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentId) {
        byte[] fileBytes = permitService.downloadDocument(documentId);
        String fileName = permitService.getDocumentFileName(documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }

    @PostMapping("/renew/{permitId}")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Renew a permit")
    public ResponseEntity<ApiResponse<PermitApplicationResponse>> renew(
            @PathVariable Long permitId,
            @Valid @RequestBody RenewPermitRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        PermitApplicationResponse response = permitService.renewPermit(permitId, request, userId);
        return ResponseEntity.ok(ApiResponse.<PermitApplicationResponse>builder()
                .success(true).message("Permit renewal submitted.").data(response).build());
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('DS','CO','ADM')")
    @Operation(summary = "Get permit processing queue")
    public ResponseEntity<ApiResponse<List<PermitApplicationResponse>>> getQueue(
            @RequestParam(required = false) PermitStatus status) {
        return ResponseEntity.ok(ApiResponse.<List<PermitApplicationResponse>>builder()
                .success(true).message("Application queue fetched successfully").data(permitService.getAll()).build());
    }

    @PutMapping("/requestDocuments/{permitId}")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Request additional documents from applicant")
    public ResponseEntity<ApiResponse<PermitApplicationResponse>> requestDocuments(
            @PathVariable Long permitId,
            @RequestBody Map<String, Object> body) {
        PermitApplicationResponse response = permitService.updateStatus(permitId, PermitStatus.PENDING_DOCUMENTS,
                "Additional documents required");
        return ResponseEntity.ok(ApiResponse.<PermitApplicationResponse>builder()
                .success(true).message("Document request sent.").data(response).build());
    }

    // PUT /{permitId}/decision  — body: {"decision":"Approved","rejectionReason":null}
    @PutMapping("/{permitId}/decision")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Approve or reject permit")
    public ResponseEntity<ApiResponse<PermitApplicationResponse>> makeDecision(
            @PathVariable Long permitId,
            @RequestBody Map<String, String> body) {
        String decision = body.get("decision");
        String rejectionReason = body.get("rejectionReason");
        PermitStatus status;
        try {
            status = PermitStatus.valueOf(decision != null ? decision.toUpperCase() : "REJECTED");
        } catch (IllegalArgumentException e) {
            status = "Approved".equalsIgnoreCase(decision) ? PermitStatus.APPROVED : PermitStatus.REJECTED;
        }
        PermitApplicationResponse response = permitService.updateStatus(permitId, status, rejectionReason);
        return ResponseEntity.ok(ApiResponse.<PermitApplicationResponse>builder()
                .success(true).message("Permit decision updated successfully").data(response).build());
    }
}
