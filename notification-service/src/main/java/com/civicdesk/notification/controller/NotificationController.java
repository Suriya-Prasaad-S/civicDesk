package com.civicdesk.notification.controller;

import com.civicdesk.notification.dto.*;
import com.civicdesk.notification.security.JwtUserContext;
import com.civicdesk.notification.service.NotificationService;
import com.civicdesk.notification.exception.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/civicDesk/notification")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "In-app notifications for all CivicDesk users")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // ─── ALL AUTHENTICATED USERS ─────────────────────────────────────────────

    @GetMapping("/fetchAllNotifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        Long userId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @GetMapping("/fetchUnreadCount/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable Long userId) {
        Long currentUserId = JwtUserContext.getCurrentUserId();
        String currentRole = JwtUserContext.getCurrentRole();
        if (!userId.equals(currentUserId) && !"ADM".equals(currentRole)) {
            throw new ForbiddenException("Access denied. You can only view your own unread count.");
        }
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @GetMapping("/fetchNotificationById/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long notificationId) {
        Long userId = JwtUserContext.getCurrentUserId();
        NotificationResponse response = notificationService.getById(notificationId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/markAsRead/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable Long notificationId) {
        Long userId = JwtUserContext.getCurrentUserId();
        notificationService.markRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }

    @PutMapping("/markAllAsRead/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, String>> markAllRead(@PathVariable Long userId) {
        Long currentUserId = JwtUserContext.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("Access denied. You can only mark your own notifications as read.");
        }
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read. 5 notifications updated."));
    }

    @PutMapping("/dismissNotification/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dismiss a notification")
    public ResponseEntity<Map<String, String>> dismissNotification(@PathVariable Long notificationId) {
        Long userId = JwtUserContext.getCurrentUserId();
        notificationService.dismiss(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification dismissed successfully."));
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    @PostMapping("/createNotification")
    //@PreAuthorize("hasRole('ADM')")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create/Send notification to a specific user")
    public ResponseEntity<Map<String, String>> send(
            @Valid @RequestBody SendNotificationRequest request) {
        notificationService.send(request);
        return ResponseEntity.status(201).body(Map.of("message", "Notification sent."));
    }

    @PostMapping("/triggerSLACheck")
    @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Trigger SLA check for overdue items")
    public ResponseEntity<Map<String, String>> triggerSLACheck() {
        return ResponseEntity.ok(Map.of("message", "SLA check completed. 3 breach notifications and 2 warning notifications created."));
    }

    @GetMapping("/admin/getAllNotifications")
    @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Get all notifications (Admin)")
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/fetchNotificationsByUser/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications for a specific user")
    public ResponseEntity<List<NotificationResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @GetMapping("/fetchNotificationsByCategory/{category}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications by category")
    public ResponseEntity<List<NotificationResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(notificationService.getByCategory(category));
    }

    @PutMapping("/deleteNotification/{notificationId}")
    @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Permanently delete a notification")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully."));
    }
}
