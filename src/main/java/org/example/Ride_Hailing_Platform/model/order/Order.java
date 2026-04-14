// src/main/java/org/example/Ride_Hailing_Platform/model/order/Order.java
package org.example.Ride_Hailing_Platform.model.order;

import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private User passenger;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private String pickupLocation;
    private String destination;
    private Double distance; // 公里
    private Double estimatedFare; // 预估费用
    private Double actualFare;

    //额外价格算法用值(目前只判断高峰期)
    private Boolean isCongestion;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private OrderType type = OrderType.STANDARD;

    //时间
    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime;
    private LocalDateTime acceptTime;

    //取消订单相关信息
    private LocalDateTime cancelTime;
    private String CancelReason;

    // 业务方法
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.ACCEPTED;
    }
}