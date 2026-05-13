package org.example.Ride_Hailing_Platform.service.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.service.DriverService;
import org.example.Ride_Hailing_Platform.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final UserService userService;
    private final DriverRepository driverRepository;

    // 司机上线/下线
    @Override
    @Transactional
    public void setDriverOnline(Long driverId, boolean isOnline) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("司机不存在"));

        driver.setIsOnline(isOnline);
        driverRepository.save(driver);
    }

    // 这个是简单版：找第一个空闲司机（后期可加距离算法）
    @Override
    public Driver findNearestAvailableDriver() {
        return driverRepository.findFirstByIsOnlineTrue().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("暂无空闲司机"));
    }

    @Override
    public Driver createDriver(String name, String phone, String password,
                               String licenseNumber, String vehicleType, String vehiclePlate) {
        User user = userService.createUser(name, phone, password, UserRole.DRIVER);

        Driver driver = (Driver) user;
        driver.setLicenseNumber(licenseNumber);
        driver.setVehicleType(vehicleType);
        driver.setVehiclePlate(vehiclePlate);
        driver.setIsOnline(false);

        return driverRepository.save(driver);
    }

    @Override
    public Driver getDriverById(Long driverId) {
        userService.findUserById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机信息不存在"));
    }

}
