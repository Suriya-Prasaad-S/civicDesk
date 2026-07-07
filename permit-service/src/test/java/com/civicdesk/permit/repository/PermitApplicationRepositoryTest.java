package com.civicdesk.permit.repository;


import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class PermitApplicationRepositoryTest {

    @Autowired
    private PermitApplicationRepository permitRepository;

    private PermitApplication permit;

    @BeforeEach
    void setUp() {

        permitRepository.deleteAll();

        permit = PermitApplication.builder()
                .citizenId(100L)
                .userId(100L)
                .permitType(PermitType.TRADE_LICENSE)
                .applicationDate(LocalDate.now())
                .propertyAddress("Tambaram")
                .validityPeriod(12)
                .fee(BigDecimal.valueOf(5000))
                .status(PermitStatus.APPLIED)
                .departmentId(1L)
                .build();

        permitRepository.save(permit);
    }

    @Test
    void findByUserId_Success() {

        List<PermitApplication> result =
                permitRepository.findByUserId(100L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findByCitizenId_Success() {

        List<PermitApplication> result =
                permitRepository.findByCitizenId(100L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void findByStatus_Success() {

        List<PermitApplication> result =
                permitRepository.findByStatus(
                        PermitStatus.APPLIED);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertEquals(
                PermitStatus.APPLIED,
                result.get(0).getStatus());
    }

    @Test
    void findByPermitType_Success() {

        List<PermitApplication> result =
                permitRepository.findByPermitType(
                        PermitType.TRADE_LICENSE);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertEquals(
                PermitType.TRADE_LICENSE,
                result.get(0).getPermitType());
    }

    @Test
    void findByDepartmentId_Success() {

        List<PermitApplication> result =
                permitRepository.findByDepartmentId(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertEquals(
                1L,
                result.get(0).getDepartmentId());
    }

    @Test
    void findByStatusAndDepartmentId_Success() {

        List<PermitApplication> result =
                permitRepository.findByStatusAndDepartmentId(
                        PermitStatus.APPLIED,
                        1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertEquals(
                PermitStatus.APPLIED,
                result.get(0).getStatus());
    }

    @Test
    void findByStatusAndExpiryDateBefore_Success() {

        permit.setStatus(PermitStatus.APPROVED);
        permit.setExpiryDate(
                LocalDate.now().plusDays(5));

        permitRepository.save(permit);

        List<PermitApplication> result =
                permitRepository.findByStatusAndExpiryDateBefore(
                        PermitStatus.APPROVED,
                        LocalDate.now().plusDays(10));

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void countPermits_Success() {

        long result =
                permitRepository.countPermits(
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(1));

        assertTrue(result > 0);
    }

    @Test
    void getDecidedPermits_Success() {

        permit.setStatus(PermitStatus.APPROVED);
        permit.setDecisionDate(LocalDate.now());

        permitRepository.save(permit);

        List<PermitApplication> result =
                permitRepository.getDecidedPermits();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}