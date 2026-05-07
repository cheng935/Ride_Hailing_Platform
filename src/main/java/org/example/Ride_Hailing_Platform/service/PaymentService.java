package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.dto.PaymentRequest;
import org.example.Ride_Hailing_Platform.dto.PaymentResponse;
import org.example.Ride_Hailing_Platform.model.payment.Payment;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request);
    PaymentResponse confirmPayment(Long paymentId);
    Payment getPaymentByOrderId(Long orderId);
}
