package org.example.ridehailing.service;

import org.example.ridehailing.dto.ProfileUpdateRequest;
import org.example.ridehailing.dto.UserDTO;
import org.example.ridehailing.model.user.Driver;

public interface DriverService {

    void setDriverOnline(Long driverId, boolean isOnline);

    Driver getDriverById(Long driverId);

    Driver findNearestAvailableDriver();

    Driver createDriver(String name, String phone, String password,
                        String licenseNumber, String vehicleType, String vehiclePlate);

    void updateVehiclePlate(Long driverId, String vehiclePlate);

    UserDTO getDriverInfo(Long userId);

    UserDTO getCurrentUserInfo(Long userId);

    UserDTO updateProfile(Long userId, ProfileUpdateRequest request);
}
