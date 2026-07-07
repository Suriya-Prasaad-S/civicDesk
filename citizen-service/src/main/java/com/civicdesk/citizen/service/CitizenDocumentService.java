package com.civicdesk.citizen.service;

import com.civicdesk.citizen.dto.CitizenDocumentRequest;
import com.civicdesk.citizen.dto.CitizenDocumentResponse;
import com.civicdesk.citizen.entity.CitizenDocument;
import com.civicdesk.citizen.entity.CitizenProfile;
import com.civicdesk.citizen.enums.DocumentStatus;
import com.civicdesk.citizen.exception.BadRequestException;
import com.civicdesk.citizen.exception.ForbiddenException;
import com.civicdesk.citizen.exception.ResourceNotFoundException;
import com.civicdesk.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.citizen.repository.CitizenProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenDocumentService {

    private final CitizenDocumentRepository documentRepository;
    private final CitizenProfileRepository profileRepository;

    @Transactional
    public CitizenDocumentResponse addDocument(CitizenDocumentRequest request, Long userId, MultipartFile file) {
        CitizenProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Please create your citizen profile first."));

        documentRepository.findByCitizenProfile_CitizenIdAndDocumentType(
                profile.getCitizenId(), request.getDocumentType())
                .ifPresent(existing -> {
                    existing.setStatus(DocumentStatus.REVOKED);
                    documentRepository.save(existing);
                    log.info("Previous {} revoked for citizenId={}", request.getDocumentType(), profile.getCitizenId());
                });

        String filePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads/citizen-docs");
                Files.createDirectories(uploadDir);
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), uploadDir.resolve(filename));
                filePath = filename;
            } catch (IOException e) {
                throw new BadRequestException("Failed to store file: " + e.getMessage());
            }
        }

        CitizenDocument doc = CitizenDocument.builder()
                .citizenProfile(profile)
                .documentType(request.getDocumentType())
                .filePath(filePath)
                .status(DocumentStatus.VALID)
                .build();

        CitizenDocument saved = documentRepository.save(doc);
        log.info("Document added: documentId={} type={} citizenId={}",
                saved.getDocumentId(), saved.getDocumentType(), profile.getCitizenId());
        return mapToResponse(saved);
    }

    public List<CitizenDocumentResponse> getMyDocuments(Long userId) {
        CitizenProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found."));
        return documentRepository.findByCitizenProfile_CitizenId(profile.getCitizenId())
                .stream().map(this::mapToResponse).toList();
    }

    public CitizenDocumentResponse getDocumentById(Long documentId) {
        CitizenDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return mapToResponse(doc);
    }

    public org.springframework.core.io.Resource loadFile(String filename) {
        try {
            java.nio.file.Path file = java.nio.file.Paths.get("uploads/citizen-docs").resolve(filename).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
            if (resource.exists()) return resource;
            throw new ResourceNotFoundException("File not found: " + filename);
        } catch (Exception e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    public List<CitizenDocumentResponse> getDocumentsByCitizenId(Long citizenId) {
        profileRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with id: " + citizenId));
        return documentRepository.findByCitizenProfile_CitizenId(citizenId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public CitizenDocumentResponse updateDocumentStatus(Long documentId, DocumentStatus status, Long userId) {
        CitizenDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        doc.setStatus(status);
        CitizenDocument updated = documentRepository.save(doc);
        log.info("Document status updated: documentId={} status={}", documentId, status);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        CitizenDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        CitizenProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found."));

        if (!doc.getCitizenProfile().getCitizenId().equals(profile.getCitizenId())) {
            throw new ForbiddenException("You can only delete your own documents.");
        }

        documentRepository.delete(doc);
        log.info("Document deleted: documentId={}", documentId);
    }

    private CitizenDocumentResponse mapToResponse(CitizenDocument doc) {
        return CitizenDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .citizenId(doc.getCitizenProfile().getCitizenId())
                .documentType(doc.getDocumentType())
                .filePath(doc.getFilePath())
                .issuedDate(doc.getIssuedDate())
                .expiryDate(doc.getExpiryDate())
                .status(doc.getStatus())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
