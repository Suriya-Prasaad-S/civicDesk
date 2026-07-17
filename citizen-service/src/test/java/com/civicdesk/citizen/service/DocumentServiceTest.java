package com.civicdesk.citizen.service;

import com.civicdesk.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.citizen.entity.CitizenDocument;
import com.civicdesk.citizen.enums.DocumentStatus;
import com.civicdesk.citizen.enums.DocumentType;
import com.civicdesk.citizen.exception.ForbiddenActionException;
import com.civicdesk.citizen.exception.InvalidRequestException;
import com.civicdesk.citizen.exception.ResourceNotFoundException;
import com.civicdesk.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.citizen.repository.CitizenProfileRepository;
import com.civicdesk.citizen.support.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentService} (the citizen document wallet). Collaborators are mocked
 * and the authenticated caller is simulated through the {@link SecurityContextHolder}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private CitizenDocumentRepository documentRepository;
    @Mock
    private CitizenProfileRepository citizenRepository;
    @Mock
    private FileStorageService fileStorage;

    @InjectMocks
    private DocumentService documentService;

    @Captor
    private ArgumentCaptor<CitizenDocument> documentCaptor;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_CIT")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static CitizenDocument document(String id, String citizenId, DocumentStatus status,
                                            LocalDate expiry) {
        CitizenDocument d = new CitizenDocument();
        d.setDocumentId(id);
        d.setCitizenId(citizenId);
        d.setDocumentType(DocumentType.BirthCertificate);
        d.setFileName("birth.pdf");
        d.setFilePath("stored-1.pdf");
        d.setFileType("pdf");
        d.setFileSizeKb(12);
        d.setStatus(status);
        d.setExpiryDate(expiry);
        return d;
    }

    // -------------------------------------------------------------------------------------------
    // addDocument (in-process push)
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("addDocument")
    class AddDocument {

        @Test
        @DisplayName("stores the bytes and records a Valid wallet entry")
        void addsSuccessfully() {
            byte[] content = "hello-world".getBytes();
            when(citizenRepository.existsById("100")).thenReturn(true);
            when(fileStorage.exists(anyString())).thenReturn(true);
            when(documentRepository.save(any(CitizenDocument.class)))
                    .thenAnswer(inv -> {
                        CitizenDocument d = inv.getArgument(0);
                        d.setDocumentId("50000001");
                        return d;
                    });

            String id = documentService.addDocument("100", "BirthCertificate", "birth.pdf",
                    content, LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1));

            assertThat(id).isEqualTo("50000001");
            verify(fileStorage).store(any(), anyString());
            verify(documentRepository).save(documentCaptor.capture());
            CitizenDocument saved = documentCaptor.getValue();
            assertThat(saved.getCitizenId()).isEqualTo("100");
            assertThat(saved.getDocumentType()).isEqualTo(DocumentType.BirthCertificate);
            assertThat(saved.getStatus()).isEqualTo(DocumentStatus.Valid);
            assertThat(saved.getFileType()).isEqualTo("pdf");
            assertThat(saved.getFileName()).isEqualTo("birth.pdf");
        }

        @Test
        @DisplayName("404 when the owning citizen does not exist")
        void citizenNotFound() {
            when(citizenRepository.existsById("100")).thenReturn(false);

            assertThatThrownBy(() -> documentService.addDocument("100", "BirthCertificate",
                    "birth.pdf", "x".getBytes(), null, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("400 for an invalid document type")
        void invalidDocumentType() {
            when(citizenRepository.existsById("100")).thenReturn(true);

            assertThatThrownBy(() -> documentService.addDocument("100", "Passport",
                    "p.pdf", "x".getBytes(), null, null))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("400 when the content is empty")
        void emptyContent() {
            when(citizenRepository.existsById("100")).thenReturn(true);

            assertThatThrownBy(() -> documentService.addDocument("100", "BirthCertificate",
                    "b.pdf", new byte[0], null, null))
                    .isInstanceOf(InvalidRequestException.class);

            verify(fileStorage, never()).store(any(), anyString());
        }

        @Test
        @DisplayName("400 and cleanup when the file does not land on disk")
        void fileNotStored() {
            when(citizenRepository.existsById("100")).thenReturn(true);
            when(fileStorage.exists(anyString())).thenReturn(false);

            assertThatThrownBy(() -> documentService.addDocument("100", "BirthCertificate",
                    "b.pdf", "x".getBytes(), null, null))
                    .isInstanceOf(InvalidRequestException.class);

            verify(fileStorage).deleteQuietly(anyString());
            verify(documentRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------------------------
    // getAllDocuments
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllDocuments")
    class GetAllDocuments {

        @Test
        @DisplayName("returns the caller's own documents")
        void returnsOwnDocuments() {
            authenticateAs("100");
            when(citizenRepository.existsById("100")).thenReturn(true);
            when(documentRepository.findByCitizenId("100"))
                    .thenReturn(List.of(document("1", "100", DocumentStatus.Valid, null)));

            List<DocumentSummaryResponse> result = documentService.getAllDocuments("100");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).documentId()).isEqualTo("1");
            assertThat(result.get(0).status()).isEqualTo("V");
        }

        @Test
        @DisplayName("a still-Valid document past its expiry reads as Expired")
        void expiredDocumentReadsAsExpired() {
            authenticateAs("100");
            when(citizenRepository.existsById("100")).thenReturn(true);
            when(documentRepository.findByCitizenId("100"))
                    .thenReturn(List.of(document("1", "100", DocumentStatus.Valid,
                            LocalDate.now().minusDays(1))));

            List<DocumentSummaryResponse> result = documentService.getAllDocuments("100");

            assertThat(result.get(0).status()).isEqualTo("E");
        }

        @Test
        @DisplayName("403 when a citizen requests another citizen's documents")
        void forbiddenForOtherCitizen() {
            authenticateAs("100");

            assertThatThrownBy(() -> documentService.getAllDocuments("200"))
                    .isInstanceOf(ForbiddenActionException.class);

            verify(documentRepository, never()).findByCitizenId(anyString());
        }

        @Test
        @DisplayName("404 when the citizen does not exist")
        void citizenNotFound() {
            authenticateAs("100");
            when(citizenRepository.existsById("100")).thenReturn(false);

            assertThatThrownBy(() -> documentService.getAllDocuments("100"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // getDocumentById / resolveDownloadFileName
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("getDocumentById / resolveDownloadFileName")
    class SingleDocument {

        @Test
        @DisplayName("returns detail for an owned document")
        void returnsDetail() {
            authenticateAs("100");
            when(documentRepository.findByDocumentIdAndCitizenId("1", "100"))
                    .thenReturn(Optional.of(document("1", "100", DocumentStatus.Valid, null)));

            DocumentDetailResponse result = documentService.getDocumentById("100", "1");

            assertThat(result.documentId()).isEqualTo("1");
            assertThat(result.citizenId()).isEqualTo("100");
            assertThat(result.filePath()).isEqualTo("stored-1.pdf");
        }

        @Test
        @DisplayName("404 when the document is not found for the citizen")
        void documentNotFound() {
            authenticateAs("100");
            when(documentRepository.findByDocumentIdAndCitizenId("1", "100"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocumentById("100", "1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("resolveDownloadFileName returns the stored file path")
        void resolvesDownloadName() {
            authenticateAs("100");
            when(documentRepository.findByDocumentIdAndCitizenId("1", "100"))
                    .thenReturn(Optional.of(document("1", "100", DocumentStatus.Valid, null)));

            assertThat(documentService.resolveDownloadFileName("100", "1")).isEqualTo("stored-1.pdf");
        }

        @Test
        @DisplayName("403 when resolving another citizen's document")
        void forbiddenForOtherCitizen() {
            authenticateAs("100");

            assertThatThrownBy(() -> documentService.resolveDownloadFileName("200", "1"))
                    .isInstanceOf(ForbiddenActionException.class);
        }
    }
}
