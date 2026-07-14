package com.civicdesk.auth.controller;

import com.civicdesk.auth.dto.response.UserDto;
import com.civicdesk.auth.entity.User;
import com.civicdesk.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/iam/users")
public class UserLookupController {

    private final UserRepository userRepository;

    public UserLookupController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId) {
        return userRepository.findById(userId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().build());
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDto> getUserByEmail(@RequestParam("email") String email) {
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().build());
    }

    @PostMapping("/batch")
    public List<UserDto> getUsersByIds(@RequestBody List<String> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::toDto)
                .toList();
    }

    private UserDto toDto(User u) {
        return new UserDto(
                u.getUserId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                u.getStatus()
        );
    }
}
