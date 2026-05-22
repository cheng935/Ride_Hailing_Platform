package org.example.ridehailing.controller;

import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.util.JwtUtil;
import org.example.ridehailing.dto.LoginRequest;
import org.example.ridehailing.dto.LoginResponse;
import org.example.ridehailing.dto.UserDTO;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.model.user.UserRole;
import org.example.ridehailing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过手机号和密码登录系统，返回JWT token")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest.getPhone(), loginRequest.getPassword());

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getPhone(),
                user.getRole().name()
        );

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .rating(user.getRating())
                .build();

        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户（乘客或司机）")
    public ApiResponse<UserDTO> register(
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam UserRole role) {
        User user = userService.createUser(name, phone, password, role);

        UserDTO dto = UserDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .rating(user.getRating())
                .build();

        if (user instanceof Driver driver) {
            dto.setIsOnline(driver.getOnline());
            dto.setLicenseNumber(driver.getLicenseNumber());
            dto.setVehicleType(driver.getVehicleType());
            dto.setVehiclePlate(driver.getVehiclePlate());
        }

        return ApiResponse.success("注册成功", dto);
    }
}
