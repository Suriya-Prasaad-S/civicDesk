package com.civicdesk.citizen.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.civicdesk.citizen.dto.response.ApiResponse;
import com.civicdesk.citizen.enums.DocumentType;
import com.civicdesk.citizen.exception.BadRequestException;
import com.civicdesk.citizen.exception.ForbiddenException;
import com.civicdesk.citizen.service.DocumentService;
import com.civicdesk.citizen.support.FileStorageService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Citizen document-wallet endpoints under base path {@code /citizenProfile}. These are the
 * citizen's read views of their wallet (government-issued documents pushed by the service-request
 * module via {@code DocumentService.addDocument}). All are CIT-only and scoped to the caller's own
 * documents. JSON responses use {@link ApiResponse}; the file download streams raw bytes.
 */
@RestController
@RequestMapping("/citizenProfile")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorage;

    /** Shared internal service-to-service key; must match the caller's {@code X-Internal-Key} header. */
    @Value("${app.internal.api-key}")
    private String internalApiKey;

    public DocumentController(DocumentService documentService, FileStorageService fileStorage) {
        this.documentService = documentService;
        this.fileStorage = fileStorage;
    }

    /**
     * POST /{citizenId}/documents (multipart/form-data) — internal service-to-service push of a
     * government-issued document into a citizen's wallet. Not citizen-facing: authenticated by the
     * shared {@code X-Internal-Key} header, not a JWT. Parts: {@code file} (the document),
     * {@code documentType} (NationalID/ResidenceProof/BirthCertificate/IncomeCertificate), and
     * optional {@code issuedDate}/{@code expiryDate} (ISO dates). Returns the new document id.
     */
    @PostMapping(value = "/{citizenId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> addDocument(
            @PathVariable String citizenId,
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "issuedDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate,
            @RequestParam(value = "expiryDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        if (internalKey == null || !internalKey.equals(internalApiKey)) {
            throw new ForbiddenException("Invalid or missing internal service key");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }
        boolean validType = Arrays.stream(DocumentType.values())
                .anyMatch(t -> t.name().equals(documentType));
        if (!validType) {
            throw new BadRequestException(
                    "documentType must be one of: NationalID, ResidenceProof, BirthCertificate, IncomeCertificate");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded document file");
        }
        String documentId = documentService.addDocument(
                citizenId, documentType, file.getOriginalFilename(), content, issuedDate, expiryDate);
        return ResponseEntity.status(201).body(ApiResponse.of("Document added successfully", documentId));
    }

    /** GET /{citizenId}/getAllDocuments — the caller's own wallet. */
    @GetMapping("/{citizenId}/getAllDocuments")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getAllDocuments(@PathVariable String citizenId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getAllDocuments(citizenId)));
    }

    /** GET /{citizenId}/getDocumentById/{documentId} — one of the caller's own documents. */
    @GetMapping("/{citizenId}/getDocumentById/{documentId}")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getDocumentById(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getDocumentById(citizenId, documentId)));
    }

    /** GET /{citizenId}/documents/{documentId}/file — streams the caller's own document bytes. */
    @GetMapping("/{citizenId}/documents/{documentId}/file")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<Resource> downloadDocumentFile(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        String filename = documentService.resolveDownloadFileName(citizenId, documentId);
        Resource resource = fileStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(contentTypeFor(extensionOf(filename)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // --- Helpers ---

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static MediaType contentTypeFor(String ext) {
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
