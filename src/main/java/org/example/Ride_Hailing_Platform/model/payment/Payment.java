package org.example.Ride_Hailing_Platform.model.payment;

import org.example.Ride_Hailing_Platform.model.order.Order;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.UNPAID;

    private String paymentMethod;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime paymentTime;

    private String transactionId;
}
