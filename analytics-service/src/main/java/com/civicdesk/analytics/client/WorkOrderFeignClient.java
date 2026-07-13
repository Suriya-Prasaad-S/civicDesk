package com.civicdesk.analytics.client;

import com.civicdesk.analytics.config.FeignClientConfig;
import com.civicdesk.analytics.dto.request.WorkOrderAnalyticsRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "work-order-client",
        url = "${app.work-order-service.url}",
        configuration = FeignClientConfig.class
)
public interface WorkOrderFeignClient {

    @PostMapping("/workorders/analytics")
    ApiResponse getAnalytics(
            @RequestBody WorkOrderAnalyticsRequest request
    );
}