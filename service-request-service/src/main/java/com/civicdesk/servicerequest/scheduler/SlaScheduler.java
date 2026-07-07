package com.civicdesk.servicerequest.scheduler;

import com.civicdesk.servicerequest.client.NotificationClient;
import com.civicdesk.servicerequest.dto.request.NotificationRequest;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final ServiceRequestRepository requestRepository;
    private final NotificationClient notificationClient;

    @Scheduled(cron = "0 0 0 * * *")
    public void processSlaBreaches() {
        LocalDate today = LocalDate.now();
        List<ServiceRequest> overdueRequests = requestRepository.findOverdueRequests(today);
        if (overdueRequests.isEmpty()) {
            log.info("SLA scheduler ran: no overdue service requests found for {}", today);
            return;
        }

        log.info("SLA scheduler found {} overdue service requests for {}", overdueRequests.size(), today);
        for (ServiceRequest request : overdueRequests) {
            try {
                request.setSlaBreach(true);
                requestRepository.save(request);

                NotificationRequest notificationPayload = NotificationRequest.builder()
                        .userId(request.getUserId())
                        .title("SLA Breach Alert")
                        .message("Your service request #" + request.getRequestId() + " has exceeded its expected completion date. Please follow up with your assigned officer.")
                        .notificationType("SLA_BREACH_ALERT")
                        .referenceId(request.getRequestId())
                        .referenceType("SERVICE_REQUEST")
                        .build();
                notificationClient.sendNotification(notificationPayload);
            } catch (Exception ex) {
                log.error("Failed to process SLA breach notification for requestId={}: {}", request.getRequestId(), ex.getMessage(), ex);
            }
        }
    }
}
