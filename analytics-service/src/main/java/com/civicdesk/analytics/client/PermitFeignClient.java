package com.civicdesk.analytics.client;

import com.civicdesk.analytics.config.FeignClientConfig;
import com.civicdesk.analytics.dto.request.PermitAnalyticsRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "permit-client",
        url = "${app.permit-service.url}",
        configuration = FeignClientConfig.class
)
public interface PermitFeignClient {

    @PostMapping("/permits/analytics")
    ApiResponse getPermitAnalytics(
            @RequestBody PermitAnalyticsRequest request
    );
}