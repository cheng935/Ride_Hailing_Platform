package org.example.ridehailing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PricingResponse {

    @JsonProperty("base_fare")
    private Double baseFare;

    @JsonProperty("distance_km")
    private Double distanceKm;

    @JsonProperty("distance_fare")
    private Double distanceFare;

    @JsonProperty("duration_minutes")
    private Double durationMinutes;

    @JsonProperty("duration_fare")
    private Double durationFare;

    private Double subtotal;

    private List<SurchargeDetail> surcharges;

    @JsonProperty("total_multiplier")
    private Double totalMultiplier;

    @JsonProperty("total_fare")
    private Double totalFare;

    @JsonProperty("capped_fare")
    private Double cappedFare;

    @JsonProperty("final_fare")
    private Double finalFare;

    @Data
    public static class SurchargeDetail {

        private String type;

        private String reason;

        private Double multiplier;

        private Double amount;
    }
}
