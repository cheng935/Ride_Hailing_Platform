// src/main/java/org/example/Ride_Hailing_Platform/model/trip/Location.java
package org.example.Ride_Hailing_Platform.model.trip;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Location {
    private Double latitude;
    private Double longitude;
    private String address;

    public Location() {}

    public Location(Double latitude, Double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
}
