package org.example.ridehailing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String pickupName;
    private Double pickupLat;
    private Double pickupLng;
    private String destName;
    private Double destLat;
    private Double destLng;
}
