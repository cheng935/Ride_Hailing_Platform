package org.example.ridehailing.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.dto.PaymentRequest;
import org.example.ridehailing.dto.PaymentResponse;
import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.order.OrderStatus;
import org.example.ridehailing.model.payment.Payment;
import org.example.ridehailing.model.payment.PaymentStatus;
import org.example.ridehailing.repository.OrderRepository;
import org.example.ridehailing.repository.PaymentRepository;
import org.example.ridehailing.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.badRequest("订单未完成，无法支付");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw BusinessException.badRequest("订单已支付");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(request.getOrderId())
                .orElse(new Payment());

        payment.setOrder(order);
        payment.setAmount(order.getActualFare());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(UUID.randomUUID().toString());

        payment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

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
                .orElseThrow(() -> BusinessException.notFound("支付记录不存在"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw BusinessException.badRequest("该支付已完成");
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
