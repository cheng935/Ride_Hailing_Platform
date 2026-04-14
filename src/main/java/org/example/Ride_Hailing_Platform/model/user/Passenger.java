package org.example.Ride_Hailing_Platform.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@DiscriminatorValue("PASSENGER")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Passenger extends User{

    private String emergencyContact;
    private int rideCount = 0;//默认为0
    //乘客独有字段待拓展

    public Passenger(String name, String phone, String password) {
        super(name, phone, password); // 调用父类构造，给name/phone/password赋值
    }
}
