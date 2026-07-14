package com.civicdesk.analytics.client;

import com.civicdesk.analytics.config.FeignClientConfig;
import com.civicdesk.analytics.dto.request.ServiceRequestAnalyticsRequest;
import com.civicdesk.analytics.dto.response.ServiceRequestAnalyticsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "service-request-service",
        path = "/civicDesk",
        configuration = FeignClientConfig.class
)
public interface ServiceRequestFeignClient {

    @PostMapping("/serviceRequest/getServiceRequestAnalytics")
    ServiceRequestAnalyticsResponse getServiceRequestAnalytics(
            @RequestBody ServiceRequestAnalyticsRequest request
    );
}