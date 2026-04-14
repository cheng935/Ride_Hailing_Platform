package org.example.Ride_Hailing_Platform.model.user;

import jakarta.persistence.*;//JPA,用于映射Java对象到数据库表
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("DRIVER")
@Getter
@Setter
public class Driver extends User{

    @Column(unique = true, nullable = false)
    private String licenseNumber;
    private Boolean isOnline = false;//默认为离线
    private String vehicleType;
    private String vehiclePlate;
}
