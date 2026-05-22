package org.example.ridehailing.controller;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final UserRepository userRepository;

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("version", "1.0.0");
        status.put("activeProfiles", Arrays.asList("dev"));
        status.put("userCount", userRepository.count());
        return ApiResponse.success(status);
    }

    @GetMapping("/test-db")
    public ApiResponse<String> testDatabase() {
        try {
            userRepository.count();
            return ApiResponse.success("Database connection successful!");
        } catch (Exception e) {
            return ApiResponse.error("Database connection failed: " + e.getMessage());
        }
    }

    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.success("Spring Boot is running successfully! Welcome to Ride Hailing Platform!");
    }
}
