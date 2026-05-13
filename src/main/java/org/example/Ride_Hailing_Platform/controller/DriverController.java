package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "司机管理", description = "司机相关接口")
public class DriverController {
    private final DriverService driverService;

    @GetMapping("/{driverId}")
    @Operation(summary = "获取司机信息", description = "根据ID获取司机详细信息")
    public ApiResponse<Driver> getDriverById(@PathVariable Long driverId) {
        Driver driver = driverService.getDriverById(driverId);
        return ApiResponse.success(driver);
    }

    @PostMapping("/create")
    @Operation(summary = "创建司机账号", description = "创建新的司机账号，包含司机专属信息")
    public ApiResponse<Driver> createDriver(
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String licenseNumber,
            @RequestParam String vehicleType,
            @RequestParam String vehiclePlate) {
        Driver driver = driverService.createDriver(name, phone, password, licenseNumber, vehicleType, vehiclePlate);
        return ApiResponse.success("司机创建成功", driver);
    }
}
