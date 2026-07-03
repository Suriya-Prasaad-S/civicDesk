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
@RequestMapping("/civicDesk/notification")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "In-app notifications for all CivicDesk users")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // ─── ALL AUTHENTICATED USERS ─────────────────────────────────────────────

    @GetMapping("/getNotifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        Long userId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true).data(notificationService.getMyNotifications(userId)).build());
    }

    @GetMapping("/getUnreadCount")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        Long userId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<UnreadCountResponse>builder()
                .success(true).data(notificationService.getUnreadCount(userId)).build());
    }

    @GetMapping("/getNotificationById/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long notificationId) {
        Long userId = JwtUserContext.getCurrentUserId();
        NotificationResponse response = notificationService.getById(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .success(true).data(response).build());
    }

    @PostMapping("/markAsRead")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @RequestBody java.util.Map<String, Long> body) {
        Long userId = JwtUserContext.getCurrentUserId();
        notificationService.markRead(body.get("notificationId"), userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Notification marked as read.").build());
    }

    @PostMapping("/markAllAsRead")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        Long userId = JwtUserContext.getCurrentUserId();
        int count = notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Integer>>builder()
                .success(true).message(count + " notification(s) marked as read.")
                .data(Map.of("markedRead", count)).build());
    }

    @PostMapping("/dismissNotification")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dismiss a notification")
    public ResponseEntity<ApiResponse<Void>> dismissNotification(
            @RequestBody java.util.Map<String, Long> body) {
        Long userId = JwtUserContext.getCurrentUserId();
        notificationService.dismiss(body.get("notificationId"), userId);
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
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
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
}
