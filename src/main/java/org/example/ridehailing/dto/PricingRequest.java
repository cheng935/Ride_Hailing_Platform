package org.example.ridehailing.dto;

import lombok.Data;

@Data
public class PricingRequest {

    private Double pickupLat;

    private Double pickupLng;

    private Double destLat;

    private Double destLng;

    private String pickupName;

    private String destName;
}
