package org.example.ridehailing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    private Long orderId;
    private String status;
    private String passengerName;
    private String passengerPhone;
    private String driverName;
    private Long driverId;
    private String driverPhone;
    private String vehiclePlate;
    private String pickupName;
    private Double pickupLat;
    private Double pickupLng;
    private String destName;
    private Double destLat;
    private Double destLng;
    private Double distance;
    private Double estimatedFare;
    private Double actualFare;
    private String paymentStatus;
    private LocalDateTime createTime;

    private Double baseFare;
    private Double distanceFare;
    private Double durationFare;
    private List<SurchargeItem> surcharges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurchargeItem {
        private String type;
        private String reason;
        private Double amount;
    }
}
