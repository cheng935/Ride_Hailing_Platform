package org.example.ridehailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.dto.PaymentRequest;
import org.example.ridehailing.dto.PaymentResponse;
import org.example.ridehailing.model.payment.Payment;
import org.example.ridehailing.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "发起支付")
    public ApiResponse<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/confirm/{paymentId}")
    @Operation(summary = "确认支付")
    public ApiResponse<PaymentResponse> confirmPayment(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.confirmPayment(paymentId);
        return ApiResponse.success(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID查询支付信息")
    public ApiResponse<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return ApiResponse.error("未找到支付信息");
        }
        return ApiResponse.success(payment);
    }
}
