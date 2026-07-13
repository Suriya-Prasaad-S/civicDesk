package com.civicdesk.analytics.client;

import com.civicdesk.analytics.config.FeignClientConfig;
import com.civicdesk.analytics.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.analytics.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "grievance-client",
        url = "${app.grievance-service.url}",
        configuration = FeignClientConfig.class
)
public interface GrievanceFeignClient {

    @PostMapping("/grievance/getGrievanceAnalytics")
    ApiResponse getGrievanceAnalytics(
            @RequestBody GrievanceAnalyticsRequest request
    );
}