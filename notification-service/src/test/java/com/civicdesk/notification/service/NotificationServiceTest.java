package com.civicdesk.notification.service;

import com.civicdesk.notification.dto.NotificationResponse;
import com.civicdesk.notification.entity.Notification;
import com.civicdesk.notification.enums.NotificationType;
import com.civicdesk.notification.exception.ForbiddenException;
import com.civicdesk.notification.repository.NotificationRepository;
import com.civicdesk.notification.client.AuditLogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogClient auditLogClient;

    @InjectMocks
    private NotificationService notificationService;

    private Notification userNotification;

    @BeforeEach
    void setUp() {
        userNotification = Notification.builder()
                .notificationId(10L)
                .userId(1L)
                .title("Test Title")
                .message("Test Message")
                .notificationType(NotificationType.GENERAL)
                .isRead(false)
                .build();
    }

    @Test
    void markRead_Success() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markRead(10L, 1L);

        assertNotNull(response);
        assertTrue(response.getIsRead());
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void markRead_Forbidden_ThrowsForbiddenException() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));

        assertThrows(ForbiddenException.class, () -> 
            notificationService.markRead(10L, 2L) // Accessing with userId 2 (owner is 1)
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getById_Success() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));

        NotificationResponse response = notificationService.getById(10L, 1L);

        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
    }

    @Test
    void getById_Forbidden_ThrowsForbiddenException() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));

        assertThrows(ForbiddenException.class, () -> 
            notificationService.getById(10L, 2L) // Accessing with userId 2 (owner is 1)
        );
    }

    @Test
    void send_Success() {
        com.civicdesk.notification.dto.SendNotificationRequest req = new com.civicdesk.notification.dto.SendNotificationRequest();
        req.setUserId(1L);
        req.setTitle("New Msg");
        req.setMessage("Msg Body");
        req.setNotificationType(NotificationType.GENERAL);

        when(notificationRepository.save(any(Notification.class))).thenReturn(userNotification);

        NotificationResponse response = notificationService.send(req);

        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void broadcast_Success() {
        com.civicdesk.notification.dto.BroadcastNotificationRequest req = new com.civicdesk.notification.dto.BroadcastNotificationRequest();
        req.setUserIds(List.of(1L, 2L));
        req.setTitle("Broad Msg");
        req.setMessage("Msg Body");
        req.setNotificationType(NotificationType.SYSTEM_ALERT);

        when(notificationRepository.saveAll(anyList())).thenReturn(List.of(userNotification));

        List<NotificationResponse> responses = notificationService.broadcast(req);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void dismiss_Success() {
        userNotification.setIsDismissed(false);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = notificationService.dismiss(10L, 1L);

        assertNotNull(response);
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void dismiss_AlreadyDismissed_ThrowsBadRequestException() {
        userNotification.setIsDismissed(true);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));

        assertThrows(com.civicdesk.notification.exception.BadRequestException.class, () ->
            notificationService.dismiss(10L, 1L)
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteNotification_Success() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(userNotification));
        doNothing().when(notificationRepository).delete(userNotification);

        assertDoesNotThrow(() -> notificationService.deleteNotification(10L));
        verify(notificationRepository, times(1)).delete(userNotification);
    }
}
