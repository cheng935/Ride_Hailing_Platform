package org.example.ridehailing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long userId;
    private String name;
    private String phone;
    private String role;
    private Double rating;

    private Boolean isOnline;
    private String licenseNumber;
    private String vehicleType;
    private String vehiclePlate;
}
