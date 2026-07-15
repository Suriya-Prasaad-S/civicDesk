package com.civicdesk.grievance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.civicdesk.grievance.config.FeignClientConfig;
import com.civicdesk.grievance.dto.request.SendNotificationRequest;

@FeignClient(
        name = "notification-service",
        // url = "${notification.service.url}",
        path = "/civicDesk",
        configuration = FeignClientConfig.class
)
public interface NotificationClient {

    @PostMapping("/notification/send")
    void sendNotification(
            @RequestBody SendNotificationRequest request
    );
}