package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.response.DocumentItemResponse;
import com.civicdesk.servicerequest.dto.response.MessageResponse;
import com.civicdesk.servicerequest.entity.RequestDocument;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.enums.VerificationStatus;
import com.civicdesk.servicerequest.exception.BadRequestException;
import com.civicdesk.servicerequest.exception.ForbiddenException;
import com.civicdesk.servicerequest.repository.RequestDocumentRepository;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestDocumentServiceTest {

    @Mock
    private RequestDocumentRepository documentRepository;

    @Mock
    private ServiceRequestRepository requestRepository;

    @InjectMocks
    private RequestDocumentService requestDocumentService;

    private ServiceRequest userRequest;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        userRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .status(RequestStatus.SUBMITTED)
                .build();

        mockFile = new MockMultipartFile(
                "file", "id_proof.pdf", "application/pdf", "dummy content".getBytes()
        );
    }

    @Test
    void uploadDocument_Success() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));

        RequestDocument savedDoc = RequestDocument.builder()
                .docSubmissionId(500L)
                .serviceRequest(userRequest)
                .documentType("ID_PROOF")
                .uploadedDate(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(documentRepository.save(any(RequestDocument.class))).thenReturn(savedDoc);

            MessageResponse response = requestDocumentService.uploadDocument(
                100L, "ID_PROOF", mockFile, 1L
        );

        assertNotNull(response);
            assertEquals("Document uploaded successfully", response.getMessage());
            DocumentItemResponse item = (DocumentItemResponse) response.getData();
            assertNotNull(item);
            assertEquals(500L, item.getDocumentId());
            assertEquals(VerificationStatus.PENDING, item.getVerificationStatus());
        verify(documentRepository, times(1)).save(any());
        verify(requestRepository, never()).save(userRequest); // Not in PENDING_DOCUMENTS
    }

    @Test
    void uploadDocument_Success_TransitionToUnderReview() {
        userRequest.setStatus(RequestStatus.PENDING_DOCUMENTS);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));

        RequestDocument savedDoc = RequestDocument.builder()
                .docSubmissionId(500L)
                .serviceRequest(userRequest)
                .documentType("ID_PROOF")
                .uploadedDate(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(documentRepository.save(any(RequestDocument.class))).thenReturn(savedDoc);

            MessageResponse response = requestDocumentService.uploadDocument(
                100L, "ID_PROOF", mockFile, 1L
        );

        assertNotNull(response);
        assertEquals(RequestStatus.UNDER_REVIEW, userRequest.getStatus()); // State transition triggered!
        verify(requestRepository, times(1)).save(userRequest);
    }

    @Test
    void uploadDocument_Forbidden_UserMismatch() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));

        assertThrows(ForbiddenException.class, () ->
            requestDocumentService.uploadDocument(100L, "ID_PROOF", mockFile, 2L) // Authenticated as 2 (owner is 1)
        );

        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_BadRequest_TerminalState() {
        userRequest.setStatus(RequestStatus.COMPLETED);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));

        assertThrows(BadRequestException.class, () ->
            requestDocumentService.uploadDocument(100L, "ID_PROOF", mockFile, 1L)
        );

        verify(documentRepository, never()).save(any());
    }

    @Test
    void getByRequestId_Citizen_Success() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));
        when(documentRepository.findByServiceRequest_RequestId(100L)).thenReturn(Collections.emptyList());

            List<DocumentItemResponse> docs = requestDocumentService.getByRequestId(100L, 1L, "CIT");
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    void getByRequestId_Citizen_Forbidden() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(userRequest));

        assertThrows(ForbiddenException.class, () ->
            requestDocumentService.getByRequestId(100L, 2L, "CIT")
        );
    }

    @Test
    void verifyDocument_Verify_Success() {
        RequestDocument doc = RequestDocument.builder()
                .docSubmissionId(500L)
                .serviceRequest(userRequest)
                .uploadedDate(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(documentRepository.findById(500L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(RequestDocument.class))).thenAnswer(i -> i.getArgument(0));

            MessageResponse response = requestDocumentService.verifyDocument(500L, VerificationStatus.VERIFIED);

        assertNotNull(response);
            DocumentItemResponse item = (DocumentItemResponse) response.getData();
            assertNotNull(item);
            assertEquals(VerificationStatus.VERIFIED, item.getVerificationStatus());
        assertEquals(RequestStatus.SUBMITTED, userRequest.getStatus()); // Does not transition
    }

    @Test
    void verifyDocument_Reject_TriggersPendingDocuments() {
        userRequest.setStatus(RequestStatus.UNDER_REVIEW);
        RequestDocument doc = RequestDocument.builder()
                .docSubmissionId(500L)
                .serviceRequest(userRequest)
                .uploadedDate(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(documentRepository.findById(500L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(RequestDocument.class))).thenAnswer(i -> i.getArgument(0));

            MessageResponse response = requestDocumentService.verifyDocument(500L, VerificationStatus.REJECTED);

        assertNotNull(response);
            DocumentItemResponse item = (DocumentItemResponse) response.getData();
            assertNotNull(item);
            assertEquals(VerificationStatus.REJECTED, item.getVerificationStatus());
        assertEquals(RequestStatus.PENDING_DOCUMENTS, userRequest.getStatus()); // Transitions back!
        verify(requestRepository, times(1)).save(userRequest);
    }
}
