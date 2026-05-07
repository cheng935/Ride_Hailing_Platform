package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.dto.PaymentRequest;
import org.example.Ride_Hailing_Platform.dto.PaymentResponse;
import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.payment.Payment;
import org.example.Ride_Hailing_Platform.model.payment.PaymentStatus;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.PaymentRepository;
import org.example.Ride_Hailing_Platform.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("订单未完成，无法支付");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("订单已支付");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(request.getOrderId())
                .orElse(new Payment());

        payment.setOrder(order);
        payment.setAmount(order.getActualFare());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(UUID.randomUUID().toString());

        payment = paymentRepository.save(payment);

        return new PaymentResponse(
                payment.getPaymentId(),
                order.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                "支付初始化成功，请确认支付"
        );
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("该支付已完成");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentTime(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        return new PaymentResponse(
                payment.getPaymentId(),
                order.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                "支付成功！"
        );
    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrder_OrderId(orderId).orElse(null);
    }
}
