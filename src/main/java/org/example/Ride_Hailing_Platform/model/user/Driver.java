package org.example.Ride_Hailing_Platform.model.user;

import jakarta.persistence.*;//JPA,用于映射Java对象到数据库表
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@DiscriminatorValue("DRIVER")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Driver extends User{

    private String licenseNumber;
    private Boolean isOnline = false;//默认为离线
    private String vehicleType;
    private String vehiclePlate;

    public Driver(String name, String phone, String password) {
        super(name, phone, password); // 调用父类构造，给name/phone/password赋值
    }
}
