package com.civicdesk.citizen.client;

import com.civicdesk.citizen.dto.request.RegisterRequest;
import com.civicdesk.citizen.dto.response.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "auth-service-client",
        url = AuthFeignClient.AUTH_SERVICE_BASE_URL
)
public interface AuthFeignClient {

    String AUTH_SERVICE_BASE_URL = "http://localhost:8081/civicDesk";

    @PostMapping("/iam/auth/register")
    void register(@RequestBody RegisterRequest request,
                  @RequestHeader(value = "X-Forwarded-For", required = false) String ip);

    @GetMapping("/iam/users/by-email")
    UserDto getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/iam/users/{userId}")
    UserDto getUserById(@PathVariable("userId") String userId);

    @PostMapping("/iam/users/batch")
    List<UserDto> getUsersByIds(@RequestBody List<String> userIds);
}
