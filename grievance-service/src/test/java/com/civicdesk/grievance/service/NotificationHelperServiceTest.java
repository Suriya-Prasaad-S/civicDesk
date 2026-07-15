package com.civicdesk.grievance.service;

import com.civicdesk.grievance.client.NotificationClient;
import com.civicdesk.grievance.dto.request.SendNotificationRequest;
import com.civicdesk.grievance.enums.NotificationType;
import com.civicdesk.grievance.enums.ReferenceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHelperServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationHelperService service;

    @Test
    void notify_Success() {

        service.notify(
                100L,
                "Assigned",
                "Review grievance",
                NotificationType.GRIEVANCE_UPDATE,
                30000001L,
                ReferenceType.GRIEVANCE);

        ArgumentCaptor<SendNotificationRequest> captor =
                ArgumentCaptor.forClass(
                        SendNotificationRequest.class);

        verify(notificationClient)
                .sendNotification(captor.capture());

        SendNotificationRequest request =
                captor.getValue();

        assertEquals(100L, request.getUserId());
        assertEquals("Assigned", request.getTitle());
        assertEquals("Review grievance", request.getMessage());
        assertEquals(NotificationType.GRIEVANCE_UPDATE,
                request.getNotificationType());
        assertEquals(30000001L,
                request.getReferenceId());
        assertEquals(ReferenceType.GRIEVANCE,
                request.getReferenceType());
    }
}