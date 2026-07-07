package com.civicdesk.citizen.service;

import com.civicdesk.citizen.dto.CitizenProfileRequest;
import com.civicdesk.citizen.dto.CitizenProfileResponse;
import com.civicdesk.citizen.entity.CitizenProfile;
import com.civicdesk.citizen.enums.CitizenStatus;
import com.civicdesk.citizen.exception.BadRequestException;
import com.civicdesk.citizen.exception.ResourceNotFoundException;
import com.civicdesk.citizen.repository.CitizenProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenProfileService {

    private final CitizenProfileRepository profileRepository;

    @Transactional
    public CitizenProfileResponse createProfile(CitizenProfileRequest request, Long userId, String email) {
        if (profileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Profile already exists for this account.");
        }
        if (profileRepository.existsByNationalIdNumber(request.getNationalIdNumber())) {
            throw new BadRequestException("National ID is already registered.");
        }

        CitizenProfile profile = CitizenProfile.builder()
                .userId(userId)
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .nationalIdNumber(request.getNationalIdNumber())
                .address(request.getAddress())
                .ward(request.getWard())
                .zone(request.getZone())
                .email(email)
                .phone(request.getPhone())
                .status(CitizenStatus.ACTIVE)
                .build();

        CitizenProfile saved = profileRepository.save(profile);
        log.info("Citizen profile created: citizenId={} userId={}", saved.getCitizenId(), userId);
        return mapToResponse(saved);
    }

    @Transactional
    public CitizenProfileResponse updateProfile(CitizenProfileRequest request, Long userId) {
        CitizenProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Please create your profile first."));

        // National ID can only be changed if it's different and not taken
        if (!profile.getNationalIdNumber().equals(request.getNationalIdNumber())
                && profileRepository.existsByNationalIdNumber(request.getNationalIdNumber())) {
            throw new BadRequestException("National ID is already registered to another citizen.");
        }

        profile.setName(request.getName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationalIdNumber(request.getNationalIdNumber());
        profile.setAddress(request.getAddress());
        profile.setWard(request.getWard());
        profile.setZone(request.getZone());
        profile.setPhone(request.getPhone());

        CitizenProfile updated = profileRepository.save(profile);
        log.info("Citizen profile updated: citizenId={}", updated.getCitizenId());
        return mapToResponse(updated);
    }

    public CitizenProfileResponse getMyProfile(Long userId) {
        CitizenProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Please complete your profile setup."));
        return mapToResponse(profile);
    }

    public CitizenProfileResponse getById(Long citizenId) {
        CitizenProfile profile = profileRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with id: " + citizenId));
        return mapToResponse(profile);
    }

    public List<CitizenProfileResponse> getAll() {
        return profileRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<CitizenProfileResponse> getByWard(String ward) {
        return profileRepository.findByWard(ward).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public CitizenProfileResponse updateStatus(Long citizenId, CitizenStatus status) {
        CitizenProfile profile = profileRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with id: " + citizenId));
        profile.setStatus(status);
        CitizenProfile updated = profileRepository.save(profile);
        log.info("Citizen status updated: citizenId={} status={}", citizenId, status);
        return mapToResponse(updated);
    }

    public CitizenProfileResponse getByCitizenIdForStaff(Long citizenId) {
        return getById(citizenId);
    }

    private CitizenProfileResponse mapToResponse(CitizenProfile p) {
        return CitizenProfileResponse.builder()
                .citizenId(p.getCitizenId())
                .userId(p.getUserId())
                .name(p.getName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .nationalIdNumber(p.getNationalIdNumber())
                .address(p.getAddress())
                .ward(p.getWard())
                .zone(p.getZone())
                .email(p.getEmail())
                .phone(p.getPhone())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
