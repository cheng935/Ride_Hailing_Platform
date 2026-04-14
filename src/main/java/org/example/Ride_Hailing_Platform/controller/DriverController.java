package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.service.DriverService;
import org.example.Ride_Hailing_Platform.service.impl.OrderServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "司机管理", description = "司机相关接口")
public class DriverController {
    private final OrderServiceImpl orderServiceImpl;
    private final DriverService driverService;

    @GetMapping("/available-orders")
    @Operation(summary = "获取可接订单", description = "获取所有待接单的订单列表")
    public ApiResponse<List<Order>> getAvailableOrders() {
        List<Order> pendingOrders = orderServiceImpl.getPendingOrders();
        return ApiResponse.success(pendingOrders);
    }

    @PutMapping("/{driverId}/orders/{orderId}/accept")
    @Operation(summary = "司机接单", description = "司机接受指定订单")
    public ApiResponse<Order> acceptOrder(
            @PathVariable Long driverId,
            @PathVariable Long orderId) {
        Order order = orderServiceImpl.acceptOrder(driverId, orderId);
        return ApiResponse.success("接单成功", order);
    }

    @PutMapping("/{driverId}/online")
    @Operation(summary = "司机上线", description = "设置司机为在线状态，可以接单")
    public ApiResponse<Void> setDriverOnline(@PathVariable Long driverId) {
        driverService.setDriverOnline(driverId, true);
        return ApiResponse.success("司机已上线", null);
    }

    @PutMapping("/{driverId}/offline")
    @Operation(summary = "司机下线", description = "设置司机为离线状态，不能接单")
    public ApiResponse<Void> setDriverOffline(@PathVariable Long driverId) {
        driverService.setDriverOnline(driverId, false);
        return ApiResponse.success("司机已下线", null);
    }

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
