package com.civicdesk.citizen.repository;

import com.civicdesk.citizen.entity.CitizenDocument;
import com.civicdesk.citizen.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenDocumentRepository extends JpaRepository<CitizenDocument, Long> {
    List<CitizenDocument> findByCitizenProfile_CitizenId(Long citizenId);
    Optional<CitizenDocument> findByCitizenProfile_CitizenIdAndDocumentType(Long citizenId, DocumentType type);
}
