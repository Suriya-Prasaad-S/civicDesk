package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.response.DocumentItemResponse;
import com.civicdesk.servicerequest.dto.response.MessageResponse;
import com.civicdesk.servicerequest.dto.request.VerifyDocumentRequest;
import com.civicdesk.servicerequest.exception.BadRequestException;
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
import java.util.Map;

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
        public ResponseEntity<MessageResponse> uploadDocument(
            @PathVariable Long requestId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        MessageResponse res = documentService.uploadDocument(
            requestId, documentType, file, JwtUserContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MessageResponse.builder()
                .message(res.getMessage())
                .build());
        }

        @GetMapping("/getDocuments/{requestId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get all documents for a request")
        public ResponseEntity<List<DocumentItemResponse>> getDocuments(
            @PathVariable Long requestId) {
        List<DocumentItemResponse> docs = documentService.getByRequestId(
            requestId, JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(docs);
        }

    @PutMapping("/verifyDocument/{docId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Verify a request document (Staff)")
    public ResponseEntity<MessageResponse> verifyDocument(
            @PathVariable Long docId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        if (request.getVerificationStatus() == null || 
            request.getVerificationStatus() == com.civicdesk.servicerequest.enums.VerificationStatus.PENDING) {
            throw new BadRequestException("Validation failed. verificationStatus must be Verified or Rejected.");
        }
        
        MessageResponse res = documentService.verifyDocument(docId, request.getVerificationStatus());
        return ResponseEntity.ok(MessageResponse.builder()
            .message(res.getMessage())
            .build());
    }
}
