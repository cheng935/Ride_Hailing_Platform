package org.example.Ride_Hailing_Platform.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("PASSENGER")
@Getter
@Setter
public class Passenger extends User{

    private String emergencyContact;
    private int rideCount = 0;//默认为0
    //乘客独有字段待拓展
}
