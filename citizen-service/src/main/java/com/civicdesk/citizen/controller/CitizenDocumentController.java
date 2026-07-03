package com.civicdesk.citizen.controller;

import com.civicdesk.citizen.dto.ApiResponse;
import com.civicdesk.citizen.dto.CitizenDocumentRequest;
import com.civicdesk.citizen.dto.CitizenDocumentResponse;
import com.civicdesk.citizen.dto.UpdateDocumentStatusRequest;
import com.civicdesk.citizen.security.JwtUserContext;
import com.civicdesk.citizen.service.CitizenDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/civicDesk/citizens")
@RequiredArgsConstructor
@Tag(name = "Citizen Document Wallet", description = "Document upload, retrieval, and verification for citizen profiles")
@SecurityRequirement(name = "BearerAuth")
public class CitizenDocumentController {

    private final CitizenDocumentService documentService;

    // ─── CITIZEN ENDPOINTS ────────────────────────────────────────────────────

    @PostMapping("/{citizenId}/uploadDocument")
    @PreAuthorize("hasAuthority('ROLE_CIT')")
    @Operation(summary = "Upload a document for citizen — multipart/form-data")
    public ResponseEntity<ApiResponse<CitizenDocumentResponse>> addDocument(
            @PathVariable Long citizenId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        CitizenDocumentRequest request = new CitizenDocumentRequest();
        request.setDocumentType(com.civicdesk.citizen.enums.DocumentType.valueOf(documentType.toUpperCase()));
        CitizenDocumentResponse doc = documentService.addDocument(request, JwtUserContext.getCurrentUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CitizenDocumentResponse>builder()
                        .success(true).message("Document uploaded successfully").data(doc).build());
    }

    @GetMapping("/{citizenId}/getAllDocuments")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Get all documents for a citizen")
    public ResponseEntity<ApiResponse<List<CitizenDocumentResponse>>> getAllDocuments(
            @PathVariable Long citizenId) {
        return ResponseEntity.ok(ApiResponse.<List<CitizenDocumentResponse>>builder()
                .success(true).message("Documents fetched").data(documentService.getDocumentsByCitizenId(citizenId)).build());
    }

    @GetMapping("/{citizenId}/getDocumentById/{documentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<ApiResponse<CitizenDocumentResponse>> getDocumentById(
            @PathVariable Long citizenId,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(ApiResponse.<CitizenDocumentResponse>builder()
                .success(true).data(documentService.getDocumentById(documentId)).build());
    }

    @PutMapping("/{citizenId}/verifyDocument/{documentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS')")
    @Operation(summary = "Verify citizen document (VERIFIED/REJECTED)")
    public ResponseEntity<ApiResponse<CitizenDocumentResponse>> updateDocumentStatus(
            @PathVariable Long citizenId,
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateDocumentStatusRequest request) {
        CitizenDocumentResponse updated = documentService.updateDocumentStatus(
                documentId, request.getStatus(), JwtUserContext.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.<CitizenDocumentResponse>builder()
                .success(true).message("Document status updated to " + request.getStatus()).data(updated).build());
    }

    @GetMapping("/files/{filename}")
    @Operation(summary = "Serve document file")
    public ResponseEntity<org.springframework.core.io.Resource> serveFile(@PathVariable String filename) {
        org.springframework.core.io.Resource resource = documentService.loadFile(filename);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
