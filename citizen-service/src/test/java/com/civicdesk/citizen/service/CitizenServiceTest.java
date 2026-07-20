package com.civicdesk.citizen.service;

import com.civicdesk.citizen.client.AuthFeignClient;
import com.civicdesk.citizen.dto.request.CitizenRegistrationRequest;
import com.civicdesk.citizen.dto.request.RegisterRequest;
import com.civicdesk.citizen.dto.request.UpdateCitizenProfileRequest;
import com.civicdesk.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.citizen.dto.response.UserDto;
import com.civicdesk.citizen.entity.CitizenProfile;
import com.civicdesk.citizen.enums.CitizenStatus;
import com.civicdesk.citizen.enums.Gender;
import com.civicdesk.citizen.enums.NotificationType;
import com.civicdesk.citizen.enums.ReferenceType;
import com.civicdesk.citizen.exception.BusinessRuleException;
import com.civicdesk.citizen.exception.DuplicateResourceException;
import com.civicdesk.citizen.exception.ForbiddenActionException;
import com.civicdesk.citizen.exception.InvalidRequestException;
import com.civicdesk.citizen.exception.ResourceNotFoundException;
import com.civicdesk.citizen.repository.CitizenProfileRepository;
import com.civicdesk.citizen.util.NationalIdUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CitizenService}. All collaborators are mocked; the authenticated
 * caller is simulated through the Spring {@link SecurityContextHolder}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitizenServiceTest {

    @Mock
    private CitizenProfileRepository citizenRepository;
    @Mock
    private AuthFeignClient authFeignClient;
    @Mock
    private AuditHelperService auditHelperService;
    @Mock
    private NotificationHelperService notificationHelperService;

    @InjectMocks
    private CitizenService citizenService;

    @Captor
    private ArgumentCaptor<CitizenProfile> profileCaptor;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static CitizenRegistrationRequest registrationRequest() {
        CitizenRegistrationRequest req = new CitizenRegistrationRequest();
        req.setName("Asha Rao");
        req.setEmail("asha@example.com");
        req.setPassword("password123");
        req.setPhone("9876543210");
        req.setDateOfBirth(LocalDate.of(1990, 5, 20));
        req.setGender("Female");
        req.setNationalIdNumber("ABCD12345678");
        req.setAddress("12 MG Road");
        req.setWard("W1");
        req.setZone("Z1");
        return req;
    }

    private static CitizenProfile profile(String userId, CitizenStatus status) {
        CitizenProfile p = new CitizenProfile();
        p.setUserId(userId);
        p.setStatus(status);
        p.setWard("W1");
        p.setZone("Z1");
        p.setAddress("12 MG Road");
        p.setGender(Gender.Female);
        p.setDateOfBirth(LocalDate.of(1990, 5, 20));
        p.setNationalIdLast4("5678");
        p.setUserProof("proof-1.pdf");
        return p;
    }

    // -------------------------------------------------------------------------------------------
    // registerCitizen
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("registerCitizen")
    class RegisterCitizen {

        @Test
        @DisplayName("creates the IAM user and an Active profile, then audits the new citizen")
        void registersSuccessfully() {
            CitizenRegistrationRequest req = registrationRequest();
            when(citizenRepository.existsByNationalIdHash(anyString())).thenReturn(false);
            when(authFeignClient.getUserByEmail("asha@example.com"))
                    .thenReturn(new UserDto("100", "Asha Rao", "asha@example.com", "9876543210", "CIT", "A"));

            citizenService.registerCitizen(req, "proof-1.pdf", "1.2.3.4");

            verify(authFeignClient).register(any(RegisterRequest.class), eq("1.2.3.4"));
            verify(citizenRepository).save(profileCaptor.capture());
            CitizenProfile saved = profileCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo("100");
            assertThat(saved.getStatus()).isEqualTo(CitizenStatus.Active);
            assertThat(saved.getGender()).isEqualTo(Gender.Female);
            assertThat(saved.getNationalIdLast4()).isEqualTo("5678");
            assertThat(saved.getNationalIdHash())
                    .isEqualTo(NationalIdUtil.hash("ABCD12345678"));
            assertThat(saved.getUserProof()).isEqualTo("proof-1.pdf");
            verify(auditHelperService).log("REGISTER_CITIZEN", "100");
        }

        @Test
        @DisplayName("rejects a national ID that is already registered (409) without creating a user")
        void rejectsDuplicateNationalId() {
            CitizenRegistrationRequest req = registrationRequest();
            when(citizenRepository.existsByNationalIdHash(anyString())).thenReturn(true);

            assertThatThrownBy(() -> citizenService.registerCitizen(req, "proof-1.pdf", "1.2.3.4"))
                    .isInstanceOf(DuplicateResourceException.class);

            verifyNoInteractions(authFeignClient);
            verify(citizenRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when IAM does not return a usable user after registration")
        void failsWhenUserCreationDoesNotResolve() {
            CitizenRegistrationRequest req = registrationRequest();
            when(citizenRepository.existsByNationalIdHash(anyString())).thenReturn(false);
            when(authFeignClient.getUserByEmail("asha@example.com")).thenReturn(null);

            assertThatThrownBy(() -> citizenService.registerCitizen(req, "proof-1.pdf", "1.2.3.4"))
                    .isInstanceOf(BusinessRuleException.class);

            verify(citizenRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an invalid gender value (400)")
        void rejectsInvalidGender() {
            CitizenRegistrationRequest req = registrationRequest();
            req.setGender("Unknown");
            when(citizenRepository.existsByNationalIdHash(anyString())).thenReturn(false);
            when(authFeignClient.getUserByEmail("asha@example.com"))
                    .thenReturn(new UserDto("100", "Asha Rao", "asha@example.com", "9876543210", "CIT", "A"));

            assertThatThrownBy(() -> citizenService.registerCitizen(req, "proof-1.pdf", "1.2.3.4"))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // getMyProfile
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfile {

        @Test
        @DisplayName("returns the current citizen's profile with a masked national id")
        void returnsProfile() {
            authenticateAs("100", "CIT");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Verified)));
            when(authFeignClient.getUserById("100"))
                    .thenReturn(new UserDto("100", "Asha Rao", "asha@example.com", "9876543210", "CIT", "A"));

            CitizenProfileResponse response = citizenService.getMyProfile();

            assertThat(response.userId()).isEqualTo("100");
            assertThat(response.name()).isEqualTo("Asha Rao");
            assertThat(response.email()).isEqualTo("asha@example.com");
            assertThat(response.nationalIdNumber()).isEqualTo("****5678");
            assertThat(response.status()).isEqualTo("V");
        }

        @Test
        @DisplayName("404 when the current citizen has no profile")
        void notFound() {
            authenticateAs("999", "CIT");
            when(citizenRepository.findById("999")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> citizenService.getMyProfile())
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("400 when there is no authenticated user in the context")
        void noAuthenticatedUser() {
            assertThatThrownBy(() -> citizenService.getMyProfile())
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // updateMyProfile
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("updateMyProfile")
    class UpdateMyProfile {

        @Test
        @DisplayName("updates only the provided mutable fields and audits")
        void updatesProvidedFields() {
            authenticateAs("100", "CIT");
            CitizenProfile existing = profile("100", CitizenStatus.Active);
            when(citizenRepository.findById("100")).thenReturn(java.util.Optional.of(existing));

            citizenService.updateMyProfile(new UpdateCitizenProfileRequest("99 New Street", null, null));

            verify(citizenRepository).save(profileCaptor.capture());
            CitizenProfile saved = profileCaptor.getValue();
            assertThat(saved.getAddress()).isEqualTo("99 New Street");
            assertThat(saved.getWard()).isEqualTo("W1"); // unchanged
            assertThat(saved.getZone()).isEqualTo("Z1"); // unchanged
            verify(auditHelperService).log("UPDATE_CITIZEN_PROFILE");
        }

        @Test
        @DisplayName("400 when no updatable field is provided")
        void rejectsEmptyUpdate() {
            authenticateAs("100", "CIT");

            assertThatThrownBy(() ->
                    citizenService.updateMyProfile(new UpdateCitizenProfileRequest(null, null, null)))
                    .isInstanceOf(InvalidRequestException.class);

            verify(citizenRepository, never()).save(any());
        }

        @Test
        @DisplayName("404 when the citizen profile does not exist")
        void notFound() {
            authenticateAs("100", "CIT");
            when(citizenRepository.findById("100")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() ->
                    citizenService.updateMyProfile(new UpdateCitizenProfileRequest("x", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // resolveProofFileName
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("resolveProofFileName")
    class ResolveProofFileName {

        @Test
        @DisplayName("citizen can resolve their own proof")
        void ownerCitizen() {
            authenticateAs("100", "CIT");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Active)));

            assertThat(citizenService.resolveProofFileName("100")).isEqualTo("proof-1.pdf");
        }

        @Test
        @DisplayName("citizen cannot resolve another citizen's proof (403)")
        void otherCitizenForbidden() {
            authenticateAs("100", "CIT");

            assertThatThrownBy(() -> citizenService.resolveProofFileName("200"))
                    .isInstanceOf(ForbiddenActionException.class);

            verify(citizenRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("an officer can resolve any citizen's proof")
        void officerAllowed() {
            authenticateAs("500", "FO");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Active)));

            assertThat(citizenService.resolveProofFileName("100")).isEqualTo("proof-1.pdf");
        }

        @Test
        @DisplayName("403 for a role that is neither owner nor officer")
        void unknownRoleForbidden() {
            authenticateAs("500", "XYZ");

            assertThatThrownBy(() -> citizenService.resolveProofFileName("100"))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        @DisplayName("404 when the citizen has no proof on file")
        void noProof() {
            authenticateAs("500", "ADM");
            CitizenProfile p = profile("100", CitizenStatus.Active);
            p.setUserProof(null);
            when(citizenRepository.findById("100")).thenReturn(java.util.Optional.of(p));

            assertThatThrownBy(() -> citizenService.resolveProofFileName("100"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // verifyCitizen
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("verifyCitizen")
    class VerifyCitizen {

        @Test
        @DisplayName("Active -> Verified stamps the officer and notifies the citizen")
        void verifiesActiveCitizen() {
            authenticateAs("500", "FO");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Active)));

            citizenService.verifyCitizen("100", new VerifyCitizenRequest("V"));

            verify(citizenRepository).save(profileCaptor.capture());
            CitizenProfile saved = profileCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(CitizenStatus.Verified);
            assertThat(saved.getVerifiedBy()).isEqualTo("500");
            assertThat(saved.getVerifiedAt()).isNotNull();
            verify(notificationHelperService).notify(
                    eq("100"), eq("Profile Verified"), anyString(),
                    eq(NotificationType.GENERAL), eq("100"), eq(ReferenceType.USER));
            verify(auditHelperService).log("VERIFY_CITIZEN");
        }

        @Test
        @DisplayName("Active -> Flagged sends the flagged notification")
        void flagsActiveCitizen() {
            authenticateAs("500", "DS");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Active)));

            citizenService.verifyCitizen("100", new VerifyCitizenRequest("F"));

            verify(citizenRepository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getStatus()).isEqualTo(CitizenStatus.Flagged);
            verify(notificationHelperService).notify(
                    eq("100"), eq("Profile Flagged"), anyString(),
                    eq(NotificationType.GENERAL), eq("100"), eq(ReferenceType.USER));
        }

        @Test
        @DisplayName("409 for an illegal transition (Verified -> Verified)")
        void rejectsIllegalTransition() {
            authenticateAs("500", "FO");
            when(citizenRepository.findById("100"))
                    .thenReturn(java.util.Optional.of(profile("100", CitizenStatus.Verified)));

            assertThatThrownBy(() -> citizenService.verifyCitizen("100", new VerifyCitizenRequest("V")))
                    .isInstanceOf(BusinessRuleException.class);

            verify(citizenRepository, never()).save(any());
            verifyNoInteractions(notificationHelperService);
        }

        @Test
        @DisplayName("400 when the verify target is 'A' (Active)")
        void rejectsActiveAsTarget() {
            authenticateAs("500", "FO");

            assertThatThrownBy(() -> citizenService.verifyCitizen("100", new VerifyCitizenRequest("A")))
                    .isInstanceOf(InvalidRequestException.class);

            verify(citizenRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("400 for an unknown status code")
        void rejectsUnknownStatusCode() {
            authenticateAs("500", "FO");

            assertThatThrownBy(() -> citizenService.verifyCitizen("100", new VerifyCitizenRequest("Z")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("404 when the citizen to verify does not exist")
        void notFound() {
            authenticateAs("500", "FO");
            when(citizenRepository.findById("100")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> citizenService.verifyCitizen("100", new VerifyCitizenRequest("V")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Listing / summaries
    // -------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("listings")
    class Listings {

        @Test
        @DisplayName("getPendingVerifications maps names from the batch user lookup")
        void pendingVerifications() {
            when(citizenRepository.findByStatus(CitizenStatus.Active))
                    .thenReturn(List.of(profile("100", CitizenStatus.Active), profile("101", CitizenStatus.Active)));
            when(authFeignClient.getUsersByIds(List.of("100", "101")))
                    .thenReturn(List.of(
                            new UserDto("100", "Asha Rao", null, null, "CIT", "A"),
                            new UserDto("101", "Bala K", null, null, "CIT", "A")));

            List<CitizenSummaryResponse> result = citizenService.getPendingVerifications();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Asha Rao");
            assertThat(result.get(1).name()).isEqualTo("Bala K");
            assertThat(result.get(0).status()).isEqualTo("A");
        }

        @Test
        @DisplayName("getAllCitizens tolerates a null batch response (name stays null)")
        void allCitizensWithNullUserBatch() {
            when(citizenRepository.findAll())
                    .thenReturn(List.of(profile("100", CitizenStatus.Verified)));
            when(authFeignClient.getUsersByIds(List.of("100"))).thenReturn(null);

            List<CitizenSummaryResponse> result = citizenService.getAllCitizens();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo("100");
            assertThat(result.get(0).name()).isNull();
        }

        @Test
        @DisplayName("getCitizensByWard maps ward-scoped profiles")
        void citizensByWard() {
            when(citizenRepository.findByWard("W1"))
                    .thenReturn(List.of(profile("100", CitizenStatus.Active)));
            when(authFeignClient.getUsersByIds(List.of("100")))
                    .thenReturn(List.of(new UserDto("100", "Asha Rao", null, null, "CIT", "A")));

            List<CitizenSummaryResponse> result = citizenService.getCitizensByWard("W1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ward()).isEqualTo("W1");
        }
    }
}
