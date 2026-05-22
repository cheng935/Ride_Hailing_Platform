// src/main/java/org/example/Ride_Hailing_Platform/model/order/Order.java
package org.example.ridehailing.model.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ridehailing.model.payment.PaymentStatus;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "passenger_id", referencedColumnName = "userId")
    private User passenger;

    @ManyToOne
    @JoinColumn(name = "driver_id", referencedColumnName = "userId")
    private Driver driver;

    private String pickupLocation;
    private String destination;
    private Double pickupLat;
    private Double pickupLng;
    private Double destLat;
    private Double destLng;
    private Double distance; // 公里
    private Double estimatedFare; // 预估费用
    private Double actualFare;

    private Double baseFare;
    private Double distanceFare;
    private Double durationFare;

    @Column(columnDefinition = "TEXT")
    private String surchargesJson;

    @Column(name = "is_congestion")
    private Boolean congestion;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private OrderType type = OrderType.STANDARD;

    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime;
    private LocalDateTime acceptTime;

    private LocalDateTime cancelTime;
    private String cancelReason;

    // 支付相关
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private LocalDateTime paymentTime;

    // 业务方法
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.ACCEPTED || status == OrderStatus.PICKING_UP;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<SurchargeItem> getSurcharges() {
        if (surchargesJson == null || surchargesJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(surchargesJson, new TypeReference<List<SurchargeItem>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void setSurcharges(List<SurchargeItem> surcharges) {
        try {
            this.surchargesJson = surcharges != null ? objectMapper.writeValueAsString(surcharges) : null;
        } catch (Exception e) {
            this.surchargesJson = null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurchargeItem {
        private String type;
        private String reason;
        private Double amount;
    }
}