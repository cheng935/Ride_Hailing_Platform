package org.example.ridehailing.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("DRIVER")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Driver extends User {

    private String licenseNumber;

    @Column(name = "is_online")
    private Boolean online = false;

    private String vehicleType;
    private String vehiclePlate;

    public Driver(String name, String phone, String password) {
        super(name, phone, password);
    }
}
