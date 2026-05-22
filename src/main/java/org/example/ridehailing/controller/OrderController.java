package org.example.ridehailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.dto.OrderDetailDTO;
import org.example.ridehailing.dto.PricingResponse;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.service.OrderService;
import org.example.ridehailing.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ride/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单相关接口")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @Operation(summary = "乘客下单", description = "创建打车订单")
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestHeader("Authorization") String auth,
            @RequestParam String pickupName,
            @RequestParam Double pickupLat,
            @RequestParam Double pickupLng,
            @RequestParam String destName,
            @RequestParam Double destLat,
            @RequestParam Double destLng) {

        Long userId = extractUserId(auth);
        Order order = orderService.createOrder(userId, pickupName, pickupLat, pickupLng,
                destName, destLat, destLng);

        PricingResponse pricing = orderService.estimatePrice(pickupLat, pickupLng,
                destLat, destLng, pickupName, destName);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("status", order.getStatus().name());
        result.put("pickupName", order.getPickupLocation());
        result.put("destName", order.getDestination());
        result.put("distance", order.getDistance());
        result.put("estimatedFare", order.getEstimatedFare());
        result.put("pricingDetail", pricing);
        return ApiResponse.success("下单成功，等待司机接单", result);
    }

    @GetMapping("/pending")
    @Operation(summary = "待接订单列表", description = "司机查看所有待接订单")
    public ApiResponse<List<OrderDetailDTO>> getPendingOrders() {
        return ApiResponse.success(orderService.getPendingOrders());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "订单详情", description = "查询单个订单状态")
    public ApiResponse<OrderDetailDTO> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderDetail(orderId));
    }

    @GetMapping("/my")
    @Operation(summary = "我的订单", description = "查看当前用户的所有订单")
    public ApiResponse<List<OrderDetailDTO>> getMyOrders(@RequestHeader("Authorization") String auth) {
        Long userId = extractUserId(auth);
        return ApiResponse.success(orderService.getMyOrders(userId));
    }

    @PutMapping("/{orderId}/accept")
    @Operation(summary = "司机接单", description = "司机接受订单")
    public ApiResponse<OrderDetailDTO> acceptOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long driverId = extractUserId(auth);
        Order order = orderService.acceptOrder(orderId, driverId);
        return ApiResponse.success("接单成功", orderService.getOrderDetail(order.getOrderId()));
    }

    @PutMapping("/{orderId}/arrive")
    @Operation(summary = "司机到达起点", description = "司机到达乘客上车点")
    public ApiResponse<String> driverArrive(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long driverId = extractUserId(auth);
        orderService.arrivePickup(orderId, driverId);
        return ApiResponse.success("已到达上车点");
    }

    @PutMapping("/{orderId}/start")
    @Operation(summary = "开始行程", description = "乘客已上车，开始行程")
    public ApiResponse<String> startTrip(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long driverId = extractUserId(auth);
        orderService.startTrip(orderId, driverId);
        return ApiResponse.success("行程开始");
    }

    @PutMapping("/{orderId}/complete")
    @Operation(summary = "完成行程", description = "到达目的地，计算实际费用")
    public ApiResponse<OrderDetailDTO> completeTrip(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long driverId = extractUserId(auth);
        OrderDetailDTO result = orderService.completeTrip(orderId, driverId);
        return ApiResponse.success("行程结束，费用：" + result.getActualFare() + "元", result);
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "发起支付", description = "发起支付请求，返回支付信息")
    public ApiResponse<OrderDetailDTO> payOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long userId = extractUserId(auth);
        OrderDetailDTO result = orderService.payOrder(orderId, userId);
        return ApiResponse.success("支付请求已发起", result);
    }

    @PostMapping("/{orderId}/confirm-pay")
    @Operation(summary = "确认支付", description = "确认支付完成")
    public ApiResponse<OrderDetailDTO> confirmPay(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId) {

        Long userId = extractUserId(auth);
        OrderDetailDTO result = orderService.confirmPay(orderId, userId);
        return ApiResponse.success("支付成功！乘车金额：" + result.getActualFare() + " 元", result);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "取消订单", description = "司机或乘客取消订单")
    public ApiResponse<String> cancelOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {

        Long userId = extractUserId(auth);
        orderService.cancelOrder(orderId, userId, reason);
        return ApiResponse.success("订单已取消");
    }

    @GetMapping("/estimate-price")
    @Operation(summary = "预估价格", description = "预估打车费用")
    public ApiResponse<PricingResponse> estimatePrice(
            @RequestParam Double pickupLat,
            @RequestParam Double pickupLng,
            @RequestParam Double destLat,
            @RequestParam Double destLng,
            @RequestParam(required = false) String pickupName,
            @RequestParam(required = false) String destName) {

        PricingResponse pricing = orderService.estimatePrice(pickupLat, pickupLng,
                destLat, destLng, pickupName, destName);
        return ApiResponse.success(pricing);
    }

    private Long extractUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }
}
