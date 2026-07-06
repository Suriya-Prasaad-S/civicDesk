package com.civicdesk.notification.service;

import com.civicdesk.notification.dto.*;
import com.civicdesk.notification.entity.Notification;
import com.civicdesk.notification.enums.ReferenceType;
import com.civicdesk.notification.exception.ForbiddenException;
import com.civicdesk.notification.exception.ResourceNotFoundException;
import com.civicdesk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ─── SEND ─────────────────────────────────────────────────────────────────

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(request.getNotificationType())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType() != null
                        ? request.getReferenceType() : ReferenceType.NONE)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent: id={} userId={} type={}", saved.getNotificationId(),
                saved.getUserId(), saved.getNotificationType());
        return mapToResponse(saved);
    }

    @Transactional
    public List<NotificationResponse> broadcast(BroadcastNotificationRequest request) {
        List<Notification> notifications = request.getUserIds().stream()
                .map(uid -> Notification.builder()
                        .userId(uid)
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .notificationType(request.getNotificationType())
                        .referenceType(ReferenceType.NONE)
                        .isRead(false)
                        .build())
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("Broadcast sent to {} users: type={}", saved.size(), request.getNotificationType());
        return saved.stream().map(this::mapToResponse).toList();
    }

    // ─── USER READ ────────────────────────────────────────────────────────────

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    public List<NotificationResponse> getMyUnread(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        return UnreadCountResponse.builder()
                .userId(userId)
                .unreadCount(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .build();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only mark your own notifications as read.");
        }

        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(Long userId) {
        int count = notificationRepository.markAllReadForUser(userId);
        log.info("Marked {} notifications as read for userId={}", count, userId);
        return count;
    }

    public NotificationResponse getById(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied. You are not authorized to view this notification.");
        }
        return mapToResponse(notification);
    }

    @Transactional
    public NotificationResponse dismiss(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only dismiss your own notifications.");
        }
        if (Boolean.TRUE.equals(notification.getIsDismissed())) {
            throw new com.civicdesk.notification.exception.BadRequestException("Notification is already dismissed.");
        }
        notification.setIsDismissed(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<NotificationResponse> getByCategory(String category) {
        String cat = category == null ? "" : category.toLowerCase();
        return notificationRepository.findAll().stream()
                .filter(n -> n.getNotificationType() != null && n.getNotificationType().name().toLowerCase().contains(cat))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToResponse).toList();
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notificationRepository.delete(notification);
        log.info("Notification deleted: id={}", notificationId);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
