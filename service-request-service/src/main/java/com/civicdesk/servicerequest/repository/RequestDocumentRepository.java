package com.civicdesk.servicerequest.repository;

import com.civicdesk.servicerequest.entity.RequestDocument;
import com.civicdesk.servicerequest.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestDocumentRepository extends JpaRepository<RequestDocument, Long> {
    List<RequestDocument> findByServiceRequest_RequestId(Long requestId);
    List<RequestDocument> findByServiceRequest_RequestIdAndVerificationStatus(Long requestId,
                                                                               VerificationStatus status);
}
