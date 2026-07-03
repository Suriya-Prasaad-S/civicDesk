package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.ApiResponse;
import com.civicdesk.servicerequest.dto.RequestDocumentResponse;
import com.civicdesk.servicerequest.dto.VerifyDocumentRequest;
import com.civicdesk.servicerequest.security.JwtUserContext;
import com.civicdesk.servicerequest.service.RequestDocumentService;
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
@RequestMapping("/civicDesk/serviceRequest")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Request Documents", description = "Upload and verify supporting documents for service requests")
public class RequestDocumentController {

    private final RequestDocumentService documentService;

    @PostMapping("/uploadDocument/{requestId}")
    @PreAuthorize("hasAuthority('ROLE_CIT')")
    @Operation(summary = "Upload document for a service request — multipart/form-data")
    public ResponseEntity<ApiResponse<RequestDocumentResponse>> uploadDocument(
            @PathVariable Long requestId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        RequestDocumentResponse doc = documentService.uploadDocument(
                requestId, documentType, file, JwtUserContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<RequestDocumentResponse>builder()
                        .success(true).message("Document uploaded successfully. Document is pending officer verification.").data(doc).build());
    }

    @GetMapping("/getDocuments/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all documents for a request")
    public ResponseEntity<ApiResponse<List<RequestDocumentResponse>>> getDocuments(
            @PathVariable Long requestId) {
        List<RequestDocumentResponse> docs = documentService.getByRequestId(
                requestId, JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(ApiResponse.<List<RequestDocumentResponse>>builder()
                .success(true).message("Documents fetched").data(docs).build());
    }

    @PutMapping("/verifyDocument/{docId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Verify a request document (Staff)")
    public ResponseEntity<ApiResponse<RequestDocumentResponse>> verifyDocument(
            @PathVariable Long docId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        RequestDocumentResponse updated = documentService.verifyDocument(docId, request.getVerificationStatus());
        return ResponseEntity.ok(ApiResponse.<RequestDocumentResponse>builder()
                .success(true).message("Document verified successfully. Document status has been updated to " + request.getVerificationStatus())
                .data(updated).build());
    }
}
