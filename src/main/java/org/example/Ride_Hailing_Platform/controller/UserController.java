package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息")
    public ApiResponse<User> getUser(@PathVariable Long userId) {
        User user = userService.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ApiResponse.success(user);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "根据手机号获取用户", description = "根据手机号查询用户信息")
    public ApiResponse<User> getUserByPhone(@PathVariable String phone) {
        User user = userService.findUserByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ApiResponse.success(user);
    }
}
