package com.civicdesk.servicerequest.config;

import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.repository.ServiceCatalogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ServiceCatalogDataLoader implements CommandLineRunner {

    private final ServiceCatalogRepository catalogRepository;

    public ServiceCatalogDataLoader(ServiceCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only insert sample catalog entries when table is empty
        if (catalogRepository.count() > 0) return;

        List<ServiceCatalog> entries = new ArrayList<>();

        // Certificates
        entries.add(ServiceCatalog.builder()
                .serviceName("Birth certificate")
                .departmentId("DPT02")
                .category(ServiceCategory.CERTIFICATE)
                .processingDays(7)
                .requiredDocuments("parent's identity,national id")
                .fee(BigDecimal.valueOf(750.00))
                .build());

        entries.add(ServiceCatalog.builder()
                .serviceName("Income certificate")
                .departmentId("DPT04")
                .category(ServiceCategory.CERTIFICATE)
                .processingDays(7)
                .requiredDocuments("PAN card,national id")
                .fee(BigDecimal.valueOf(150.00))
                .build());

        // Utilities
        entries.add(ServiceCatalog.builder()
                .serviceName("New water supply")
                .departmentId("DPT04")
                .category(ServiceCategory.UTILITY)
                .processingDays(14)
                .requiredDocuments("property ownership proof & id,proof of residence")
                .fee(BigDecimal.valueOf(1000.00))
                .build());

        entries.add(ServiceCatalog.builder()
                .serviceName("Drainage connection")
                .departmentId("DPT04")
                .category(ServiceCategory.UTILITY)
                .processingDays(14)
                .requiredDocuments("national id,proof of residence(property tax)")
                .fee(BigDecimal.valueOf(500.00))
                .build());

        // Registration
        entries.add(ServiceCatalog.builder()
                .serviceName("Voter id")
                .departmentId("DPT04")
                .category(ServiceCategory.REGISTRATION)
                .processingDays(21)
                .requiredDocuments("national id,age proof(birth certificate),proof of residence(adhaar)")
                .fee(BigDecimal.valueOf(150.00))
                .build());

        entries.add(ServiceCatalog.builder()
                .serviceName("Ration card")
                .departmentId("DPT04")
                .category(ServiceCategory.REGISTRATION)
                .processingDays(21)
                .requiredDocuments("national id of head,income certificate,proof of residence,national id of all")
                .fee(BigDecimal.valueOf(250.00))
                .build());

        // Welfare
        entries.add(ServiceCatalog.builder()
                .serviceName("Old age pension")
                .departmentId("DPT04")
                .category(ServiceCategory.WELFARE)
                .processingDays(30)
                .requiredDocuments("age proof,national id,income certificate")
                .fee(BigDecimal.valueOf(50.00))
                .build());

        entries.add(ServiceCatalog.builder()
                .serviceName("Scholarship")
                .departmentId("DPT04")
                .category(ServiceCategory.WELFARE)
                .processingDays(30)
                .requiredDocuments("national id,income certificate of parents,community certificate,previous yr marksheet,bank details,college id proof")
                .fee(BigDecimal.valueOf(750.00))
                .build());

        catalogRepository.saveAll(entries);
    }
}
