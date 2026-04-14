// src/main/java/org/example/Ride_Hailing_Platform/model/trip/Trip.java
package org.example.Ride_Hailing_Platform.model.trip;

import org.example.Ride_Hailing_Platform.model.order.Order;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripId;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "pickup_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "pickup_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "pickup_longitude"))
    })
    private Location pickupLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "destination_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "destination_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "destination_longitude"))
    })
    private Location destinationLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "current_address")),
            @AttributeOverride(name = "latitude", column = @Column(name = "current_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "current_longitude"))
    })
    private Location currentLocation;

    private Double actualDistance; // 实际行驶距离
    private Double actualFare; // 实际费用
    private Integer rating; // 评分(1-5)
    private String feedback; // 评价

    @Enumerated(EnumType.STRING)
    private TripStatus status = TripStatus.NOT_STARTED;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime;

    // 业务方法
    public boolean isCompleted() {
        return status == TripStatus.COMPLETED;
    }

    public void startTrip() {
        this.status = TripStatus.IN_PROGRESS;
        this.startTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public void completeTrip(Double actualDistance, Double actualFare, Integer rating, String feedback) {
        this.status = TripStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.actualDistance = actualDistance;
        this.actualFare = actualFare;
        this.rating = rating;
        this.feedback = feedback;
        this.updateTime = LocalDateTime.now();
    }

    // 可选：在设置位置时自动更新时间
    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
        this.updateTime = LocalDateTime.now(); // 自动更新时间
    }
}