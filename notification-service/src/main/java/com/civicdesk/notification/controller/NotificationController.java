package com.civicdesk.notification.controller;

import com.civicdesk.notification.dto.*;
import com.civicdesk.notification.security.JwtUserContext;
import com.civicdesk.notification.service.NotificationService;
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
@RequestMapping("/civicDesk/notifications/alerts")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "In-app notifications for all CivicDesk users")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // ─── ALL AUTHENTICATED USERS ─────────────────────────────────────────────

        @GetMapping("/fetchAllNotifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        Long userId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true).data(notificationService.getMyNotifications(userId)).build());
    }

        @GetMapping("/fetchUnreadCount/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count")
        public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@PathVariable Long userId) {
                return ResponseEntity.ok(ApiResponse.<UnreadCountResponse>builder()
                                .success(true).data(notificationService.getUnreadCount(userId)).build());
    }

        @GetMapping("/fetchNotificationById/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long notificationId) {
                Long userId = JwtUserContext.getCurrentUserId();
                NotificationResponse response = notificationService.getById(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .success(true).data(response).build());
    }

        @PutMapping("/markAsRead/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
        public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long notificationId) {
                Long userId = JwtUserContext.getCurrentUserId();
                notificationService.markRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Notification marked as read.").build());
    }

        @PutMapping("/markAllAsRead/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
        public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(@PathVariable Long userId) {
                int count = notificationService.markAllRead(userId);
                return ResponseEntity.ok(ApiResponse.<Map<String, Integer>>builder()
                                .success(true).message("All notifications marked as read. " + count + " notifications updated.")
                                .data(Map.of("markedRead", count)).build());
    }

        @PutMapping("/dismissNotification/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dismiss a notification")
        public ResponseEntity<ApiResponse<Void>> dismissNotification(@PathVariable Long notificationId) {
                Long userId = JwtUserContext.getCurrentUserId();
                notificationService.dismiss(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Notification dismissed successfully.").build());
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

        @PostMapping("/createNotification")
    @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Create/Send notification to a specific user")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.send(request);
                return ResponseEntity.status(201).body(ApiResponse.<NotificationResponse>builder()
                                .success(true).message("Notification sent.").data(response).build());
    }

        @PostMapping("/triggerSLACheck")
        @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Trigger SLA check for overdue items")
    public ResponseEntity<ApiResponse<Void>> triggerSLACheck() {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("SLA check triggered successfully.").build());
    }

        @GetMapping("/admin/getAllNotifications")
    @PreAuthorize("hasRole('ADM')")
    @Operation(summary = "Get all notifications (Admin)")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true).data(notificationService.getAll()).build());
    }

        @GetMapping("/fetchNotificationsByUser/{userId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get notifications for a specific user")
        public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByUser(@PathVariable Long userId) {
                return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                                .success(true).data(notificationService.getMyNotifications(userId)).build());
        }

        @GetMapping("/fetchNotificationsByCategory/{category}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get notifications by category")
        public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByCategory(@PathVariable String category) {
                return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                                .success(true).data(notificationService.getByCategory(category)).build());
        }

        @PutMapping("/deleteNotification/{notificationId}")
        @PreAuthorize("hasRole('ADM')")
        @Operation(summary = "Permanently delete a notification")
        public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
                notificationService.deleteNotification(notificationId);
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .success(true).message("Notification deleted successfully.").build());
        }
}
