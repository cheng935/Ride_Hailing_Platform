package org.example.Ride_Hailing_Platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.dto.PaymentRequest;
import org.example.Ride_Hailing_Platform.dto.PaymentResponse;
import org.example.Ride_Hailing_Platform.model.payment.Payment;
import org.example.Ride_Hailing_Platform.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付管理", description = "支付相关接口")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "发起支付")
    @PostMapping("/initiate")
    public ApiResponse<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.initiatePayment(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "确认支付")
    @PostMapping("/confirm/{paymentId}")
    public ApiResponse<PaymentResponse> confirmPayment(@PathVariable Long paymentId) {
        try {
            PaymentResponse response = paymentService.confirmPayment(paymentId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "根据订单ID查询支付信息")
    @GetMapping("/order/{orderId}")
    public ApiResponse<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return ApiResponse.error("未找到支付信息");
        }
        return ApiResponse.success(payment);
    }
}
