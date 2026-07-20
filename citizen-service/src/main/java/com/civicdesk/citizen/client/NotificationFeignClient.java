package com.civicdesk.citizen.client;

import com.civicdesk.citizen.config.NotificationFeignConfig;
import com.civicdesk.citizen.dto.request.SendNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for notification-service. Uses a hardcoded base URL (matching this service's
 * existing {@link AuthFeignClient} convention) and a per-client interceptor that attaches the
 * {@code X-Service-Token} internal header (see {@link NotificationFeignConfig}).
 */
@FeignClient(
        name = "notification-service",
        // url = NotificationFeignClient.NOTIFICATION_SERVICE_BASE_URL,
        path = "/civicDesk",
        configuration = NotificationFeignConfig.class
)
public interface NotificationFeignClient {

    // String NOTIFICATION_SERVICE_BASE_URL = "http://localhost:8087/civicDesk";

    @PostMapping("/notification/send")
    void sendNotification(@RequestBody SendNotificationRequest request);
}
