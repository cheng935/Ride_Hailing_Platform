package org.example.ridehailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.dto.ProfileUpdateRequest;
import org.example.ridehailing.dto.UserDTO;
import org.example.ridehailing.service.DriverService;
import org.example.ridehailing.service.pubsub.RedisPubSubService;
import org.example.ridehailing.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ride/driver")
@RequiredArgsConstructor
@Tag(name = "司机管理", description = "司机相关接口")
public class DriverController {

    private final DriverService driverService;
    private final RedisPubSubService redisPubSubService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "根据Token获取当前登录用户信息")
    public ApiResponse<UserDTO> me(@RequestHeader("Authorization") String auth) {
        Long userId = extractUserId(auth);
        return ApiResponse.success(driverService.getCurrentUserInfo(userId));
    }

    @PutMapping("/online")
    @Operation(summary = "司机上线", description = "司机设置为可接单状态")
    public ApiResponse<String> driverOnline(@RequestHeader("Authorization") String auth) {
        Long driverId = extractUserId(auth);
        driverService.setDriverOnline(driverId, true);
        redisPubSubService.publishDriverEvent("ONLINE", driverId, null);
        return ApiResponse.success("已上线，等待接单");
    }

    @PutMapping("/offline")
    @Operation(summary = "司机下线", description = "司机设置为离线状态")
    public ApiResponse<String> driverOffline(@RequestHeader("Authorization") String auth) {
        Long driverId = extractUserId(auth);
        driverService.setDriverOnline(driverId, false);
        redisPubSubService.publishDriverEvent("OFFLINE", driverId, null);
        return ApiResponse.success("已下线");
    }

    @PutMapping("/vehicle-plate")
    @Operation(summary = "更新车牌号", description = "司机更新自己的车牌号")
    public ApiResponse<String> updateVehiclePlate(
            @RequestHeader("Authorization") String auth,
            @RequestParam String vehiclePlate) {

        Long driverId = extractUserId(auth);
        if (vehiclePlate == null || vehiclePlate.trim().isEmpty()) {
            return ApiResponse.error("车牌号不能为空");
        }
        driverService.updateVehiclePlate(driverId, vehiclePlate.trim());
        return ApiResponse.success("车牌号已更新");
    }

    @GetMapping("/info")
    @Operation(summary = "司机信息", description = "获取司机详细信息")
    public ApiResponse<UserDTO> getDriverInfo(@RequestHeader("Authorization") String auth) {
        Long userId = extractUserId(auth);
        return ApiResponse.success(driverService.getDriverInfo(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人信息", description = "更新姓名、密码、车型、车牌等")
    public ApiResponse<UserDTO> updateProfile(
            @RequestHeader("Authorization") String auth,
            @RequestBody ProfileUpdateRequest request) {

        Long userId = extractUserId(auth);
        UserDTO updated = driverService.updateProfile(userId, request);
        return ApiResponse.success("个人信息已更新", updated);
    }

    private Long extractUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
