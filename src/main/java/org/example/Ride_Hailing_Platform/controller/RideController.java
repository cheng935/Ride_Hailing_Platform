package org.example.Ride_Hailing_Platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.config.JwtUtil;
import org.example.Ride_Hailing_Platform.dto.PaymentRequest;
import org.example.Ride_Hailing_Platform.dto.PaymentResponse;
import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.order.OrderType;
import org.example.Ride_Hailing_Platform.model.payment.Payment;
import org.example.Ride_Hailing_Platform.model.payment.PaymentStatus;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ride")
@RequiredArgsConstructor
@Tag(name = "打车流程", description = "完整的打车业务流程接口")
public class RideController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;


    // ==================== 当前用户 ====================

    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "根据Token获取当前登录用户信息")
    public ApiResponse<Map<String, Object>> me(@RequestHeader("Authorization") String auth) {
        try {
            String token = auth.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ApiResponse.error("用户不存在");

            Map<String, Object> result = new HashMap<>();
            result.put("userId", user.getUserId());
            result.put("name", user.getName());
            result.put("phone", user.getPhone());
            result.put("role", user.getRole().name());
            result.put("rating", user.getRating());

            if (user instanceof Driver driver) {
                result.put("isOnline", driver.getIsOnline());
                result.put("vehiclePlate", driver.getVehiclePlate());
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("Token无效");
        }
    }

    // ==================== 司机上下线 ====================

    @PutMapping("/driver/online")
    @Operation(summary = "司机上线", description = "司机设置为可接单状态")
    public ApiResponse<String> driverOnline(@RequestHeader("Authorization") String auth) {
        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");

        driver.setIsOnline(true);
        driverRepository.save(driver);
        return ApiResponse.success("已上线，等待接单");
    }

    @PutMapping("/driver/offline")
    @Operation(summary = "司机下线", description = "司机设置为离线状态")
    public ApiResponse<String> driverOffline(@RequestHeader("Authorization") String auth) {
        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");

        driver.setIsOnline(false);
        driverRepository.save(driver);
        return ApiResponse.success("已下线");
    }

    // ==================== 订单流程 ====================

    @PostMapping("/order")
    @Operation(summary = "乘客下单", description = "创建打车订单")
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestHeader("Authorization") String auth,
            @RequestParam String pickupName,
            @RequestParam Double pickupLat,
            @RequestParam Double pickupLng,
            @RequestParam String destName,
            @RequestParam Double destLat,
            @RequestParam Double destLng) {

        Long userId = jwtUtil.extractUserId(auth.replace("Bearer ", ""));
        User passenger = userRepository.findById(userId).orElse(null);
        if (passenger == null) return ApiResponse.error("用户不存在");

        List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED,
                OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS);
        List<Order> activeOrders = orderRepository.findByPassengerAndStatusIn(passenger, activeStatuses);
        if (!activeOrders.isEmpty()) {
            return ApiResponse.error("您有未完成的订单");
        }

        double distance = haversineDistance(pickupLat, pickupLng, destLat, destLng);
        double fare = Math.round((8.0 + distance * 2.5) * 100.0) / 100.0;

        Order order = new Order();
        order.setPassenger(passenger);
        order.setPickupLocation(pickupName);
        order.setPickupLat(pickupLat);
        order.setPickupLng(pickupLng);
        order.setDestination(destName);
        order.setDestLat(destLat);
        order.setDestLng(destLng);
        order.setDistance(Math.round(distance * 100.0) / 100.0);
        order.setEstimatedFare(fare);
        order.setStatus(OrderStatus.PENDING);
        order.setType(OrderType.STANDARD);
        order.setCreateTime(LocalDateTime.now());

        order = orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("status", order.getStatus().name());
        result.put("pickupName", order.getPickupLocation());
        result.put("destName", order.getDestination());
        result.put("distance", order.getDistance());
        result.put("estimatedFare", order.getEstimatedFare());
        return ApiResponse.success("下单成功，等待司机接单", result);
    }

    @GetMapping("/orders/pending")
    @Operation(summary = "待接订单列表", description = "司机查看所有待接订单")
    public ApiResponse<List<Map<String, Object>>> getPendingOrders() {
        List<Order> orders = orderRepository.findByStatusIn(List.of(OrderStatus.PENDING));
        List<Map<String, Object>> list = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("passengerName", o.getPassenger().getName());
            m.put("pickupName", o.getPickupLocation());
            m.put("pickupLat", o.getPickupLat());
            m.put("pickupLng", o.getPickupLng());
            m.put("destName", o.getDestination());
            m.put("destLat", o.getDestLat());
            m.put("destLng", o.getDestLng());
            m.put("distance", o.getDistance());
            m.put("estimatedFare", o.getEstimatedFare());
            m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "订单详情", description = "查询单个订单状态")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable Long orderId) {
        Order o = orderRepository.findById(orderId).orElse(null);
        if (o == null) return ApiResponse.error("订单不存在");

        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("status", o.getStatus().name());
        m.put("passengerName", o.getPassenger() != null ? o.getPassenger().getName() : null);
        m.put("driverName", o.getDriver() != null ? o.getDriver().getName() : null);
        m.put("driverPhone", o.getDriver() != null ? o.getDriver().getPhone() : null);
        m.put("vehiclePlate", o.getDriver() != null ? o.getDriver().getVehiclePlate() : null);
        m.put("pickupName", o.getPickupLocation());
        m.put("pickupLat", o.getPickupLat());
        m.put("pickupLng", o.getPickupLng());
        m.put("destName", o.getDestination());
        m.put("destLat", o.getDestLat());
        m.put("destLng", o.getDestLng());
        m.put("distance", o.getDistance());
        m.put("estimatedFare", o.getEstimatedFare());
        m.put("actualFare", o.getActualFare());
        m.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID");
        m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
        return ApiResponse.success(m);
    }

    @GetMapping("/orders/my")
    @Operation(summary = "我的订单", description = "查看当前用户的所有订单")
    public ApiResponse<List<Map<String, Object>>> getMyOrders(@RequestHeader("Authorization") String auth) {
        Long userId = jwtUtil.extractUserId(auth.replace("Bearer ", ""));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ApiResponse.error("用户不存在");

        List<Order> orders;
        if (user instanceof Driver) {
            orders = orderRepository.findRecentOrdersByDriver(userId);
        } else {
            orders = orderRepository.findRecentOrdersByPassenger(userId);
        }

        List<Map<String, Object>> list = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("status", o.getStatus().name());
            m.put("pickupName", o.getPickupLocation());
            m.put("destName", o.getDestination());
            m.put("estimatedFare", o.getEstimatedFare());
            m.put("actualFare", o.getActualFare());
            m.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID");
            m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @PutMapping("/order/{orderId}/accept")
    @Operation(summary = "司机接单", description = "司机接受订单")
    public ApiResponse<Map<String, Object>> acceptOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");
        if (!driver.getIsOnline()) return ApiResponse.error("请先上线");

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING) return ApiResponse.error("订单已被接走");

        order.setDriver(driver);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptTime(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("status", order.getStatus().name());
        result.put("passengerName", order.getPassenger().getName());
        result.put("pickupName", order.getPickupLocation());
        result.put("pickupLat", order.getPickupLat());
        result.put("pickupLng", order.getPickupLng());
        result.put("destName", order.getDestination());
        result.put("destLat", order.getDestLat());
        result.put("destLng", order.getDestLng());
        return ApiResponse.success("接单成功", result);
    }

    @PutMapping("/order/{orderId}/arrive")
    @Operation(summary = "司机到达起点", description = "司机到达乘客上车点")
    public ApiResponse<String> driverArrive(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getStatus() != OrderStatus.ACCEPTED) return ApiResponse.error("订单状态不正确");

        order.setStatus(OrderStatus.PICKING_UP);
        orderRepository.save(order);
        return ApiResponse.success("已到达上车点");
    }

    @PutMapping("/order/{orderId}/start")
    @Operation(summary = "开始行程", description = "乘客已上车，开始行程")
    public ApiResponse<String> startTrip(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getStatus() != OrderStatus.PICKING_UP) return ApiResponse.error("订单状态不正确");

        order.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(order);
        return ApiResponse.success("行程开始");
    }

    @PutMapping("/order/{orderId}/complete")
    @Operation(summary = "完成行程", description = "到达目的地，计算实际费用")
    public ApiResponse<Map<String, Object>> completeTrip(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Driver driver = getDriverFromToken(auth);
        if (driver == null) return ApiResponse.error("不是司机账号");

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getStatus() != OrderStatus.IN_PROGRESS) return ApiResponse.error("订单状态不正确");

        double actualFare = order.getEstimatedFare() != null ? order.getEstimatedFare() : 20.0;
        order.setActualFare(actualFare);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("status", order.getStatus().name());
        result.put("actualFare", actualFare);
        return ApiResponse.success("行程结束，费用：" + actualFare + "元", result);
    }

    @PostMapping("/order/{orderId}/pay")
    @Operation(summary = "发起支付", description = "发起支付请求，返回支付信息。实际生产环境需对接微信/支付宝SDK")
    public ApiResponse<Map<String, Object>> payOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long userId = jwtUtil.extractUserId(auth.replace("Bearer ", ""));

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getPassenger() == null || !order.getPassenger().getUserId().equals(userId)) {
            return ApiResponse.error("无权支付此订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) return ApiResponse.error("订单未完成");
        if (order.getPaymentStatus() == PaymentStatus.PAID) return ApiResponse.error("已支付");

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(orderId);
        req.setPaymentMethod("微信支付");

        // 仅发起支付，生成支付记录（状态：PENDING）
        // 生产环境此处应调用第三方支付SDK，获取支付链接/二维码
        PaymentResponse payResp = paymentService.initiatePayment(req);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("paymentId", payResp.getPaymentId());
        result.put("amount", order.getActualFare());
        result.put("status", "PENDING");
        result.put("message", "支付请求已发起，请确认支付");
        return ApiResponse.success(result);
    }

    @PostMapping("/order/{orderId}/confirm-pay")
    @Operation(summary = "确认支付", description = "确认支付完成。模拟场景：点击确认即完成支付；生产环境需验证第三方支付回调")
    public ApiResponse<Map<String, Object>> confirmPay(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long userId = jwtUtil.extractUserId(auth.replace("Bearer ", ""));

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (order.getPassenger() == null || !order.getPassenger().getUserId().equals(userId)) {
            return ApiResponse.error("无权操作此订单");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) return ApiResponse.error("已支付");

        // 模拟：直接确认支付成功
        // 生产环境此处应验证第三方支付回调签名，确认款项已到账后再调用
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) return ApiResponse.error("未找到支付记录，请先发起支付");

        paymentService.confirmPayment(payment.getPaymentId());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("amount", order.getActualFare());
        result.put("status", "PAID");
        result.put("message", "支付成功！乘车金额：" + order.getActualFare() + " 元");
        return ApiResponse.success(result);
    }

    // ==================== 工具方法 ====================

    private Driver getDriverFromToken(String auth) {
        try {
            String token = auth.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user instanceof Driver driver) return driver;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}