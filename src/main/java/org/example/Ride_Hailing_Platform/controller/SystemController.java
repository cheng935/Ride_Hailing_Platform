package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {

    private final UserRepository userRepository;

    @Autowired
    public SystemController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("version", "1.0.0");
        status.put("activeProfiles", Arrays.asList("dev"));
        status.put("userCount", userRepository.count());
        return status;
    }

    @GetMapping("/test-db")
    public String testDatabase() {
        try {
            userRepository.count(); // 执行简单查询
            return "✅ Database connection successful!";
        } catch (Exception e) {
            return "❌ Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/hello")
    public String hello() {
        return "🎉 Spring Boot is running successfully! Welcome to Ride Hailing Platform!";
    }
}