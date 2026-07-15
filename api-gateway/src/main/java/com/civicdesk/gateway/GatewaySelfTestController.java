package com.civicdesk.gateway;

import com.civicdesk.gateway.filter.RateLimitingFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class GatewaySelfTestController {

    private final RateLimitingFilter rateLimitingFilter;

    public GatewaySelfTestController(RateLimitingFilter rateLimitingFilter) {
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @GetMapping("/gateway-test/reset")
    public ResponseEntity<String> reset() {
        rateLimitingFilter.reset();
        return ResponseEntity.ok("reset");
    }
}
