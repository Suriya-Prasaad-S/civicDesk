package com.civicdesk.analytics.client;

import com.civicdesk.analytics.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.analytics.dto.response.GrievanceAnalyticsResponse;
import com.civicdesk.analytics.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrievanceClient {

    private final RestTemplate restTemplate;

    @Value("${app.grievance-service.url}")
    private String grievanceServiceUrl;

    /**
     * Get grievance analytics from grievance-service
     */
    public GrievanceAnalyticsResponse getGrievanceAnalytics(GrievanceAnalyticsRequest request) {
        String jwtToken = getCurrentJwtToken();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null) {
                headers.set("Authorization", jwtToken);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            String endpoint = buildEndpoint(request);

            log.info("Fetching grievance analytics from {}", endpoint);
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    requestEntity,
                    ApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Grievance analytics fetched successfully");
                // Convert the response data to GrievanceAnalyticsResponse
                if (response.getBody().getData() != null) {
                    // Spring's RestTemplate will deserialize to Map, convert to proper object
                    Object data = response.getBody().getData();
                    if (data instanceof java.util.Map) {
                        return convertMapToResponse((java.util.Map<String, Object>) data);
                    } else if (data instanceof GrievanceAnalyticsResponse) {
                        return (GrievanceAnalyticsResponse) data;
                    }
                }
            } else {
                log.warn("Failed to fetch grievance analytics. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error fetching grievance analytics: {}", e.getMessage(), e);
        }

        return GrievanceAnalyticsResponse.builder()
                .totalGrievances(0L)
                .build();
    }

    private String buildEndpoint(GrievanceAnalyticsRequest request) {
        StringBuilder sb = new StringBuilder(grievanceServiceUrl)
                .append("/civicDesk/grievance/analytics");
        
        if (request.getDeptId() != null) {
            sb.append("?deptId=").append(request.getDeptId());
        }
        if (request.getFromDate() != null) {
            sb.append("&fromDate=").append(request.getFromDate());
        }
        if (request.getToDate() != null) {
            sb.append("&toDate=").append(request.getToDate());
        }
        
        return sb.toString();
    }

    private GrievanceAnalyticsResponse convertMapToResponse(java.util.Map<String, Object> data) {
        return GrievanceAnalyticsResponse.builder()
                .totalGrievances(getLongValue(data.get("totalGrievances")))
                .statusBreakdown((java.util.List<GrievanceAnalyticsResponse.LabelCountDto>) data.getOrDefault("statusBreakdown", java.util.List.of()))
                .categoryBreakdown((java.util.List<GrievanceAnalyticsResponse.LabelCountDto>) data.getOrDefault("categoryBreakdown", java.util.List.of()))
                .assignmentBreakdown((java.util.List<GrievanceAnalyticsResponse.LabelCountDto>) data.getOrDefault("assignmentBreakdown", java.util.List.of()))
                .escalationBreakdown((java.util.List<GrievanceAnalyticsResponse.LabelCountDto>) data.getOrDefault("escalationBreakdown", java.util.List.of()))
                .trend((java.util.List<GrievanceAnalyticsResponse.TrendDto>) data.getOrDefault("trend", java.util.List.of()))
                .build();
    }

    private Long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private String getCurrentJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("Authorization");
            }
        } catch (Exception e) {
            log.warn("Failed to extract JWT token from current request", e);
        }
        return null;
    }
}
