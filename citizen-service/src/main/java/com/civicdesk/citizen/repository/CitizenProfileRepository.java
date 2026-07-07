package com.civicdesk.citizen.repository;

import com.civicdesk.citizen.entity.CitizenProfile;
import com.civicdesk.citizen.enums.CitizenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, Long> {
    Optional<CitizenProfile> findByUserId(Long userId);
    Optional<CitizenProfile> findByEmail(String email);
    boolean existsByUserId(Long userId);
    boolean existsByNationalIdNumber(String nationalIdNumber);
    List<CitizenProfile> findByStatus(CitizenStatus status);
    List<CitizenProfile> findByWard(String ward);
}
