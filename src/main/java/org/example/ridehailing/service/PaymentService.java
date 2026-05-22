package org.example.ridehailing.service;

import org.example.ridehailing.dto.PaymentRequest;
import org.example.ridehailing.dto.PaymentResponse;
import org.example.ridehailing.model.payment.Payment;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request);
    PaymentResponse confirmPayment(Long paymentId);
    Payment getPaymentByOrderId(Long orderId);
}
