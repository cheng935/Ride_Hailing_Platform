package org.example.ridehailing.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.dto.ProfileUpdateRequest;
import org.example.ridehailing.dto.UserDTO;
import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.model.user.UserRole;
import org.example.ridehailing.repository.DriverRepository;
import org.example.ridehailing.service.DriverService;
import org.example.ridehailing.service.UserService;
import org.example.ridehailing.service.cache.DriverOnlineCacheService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final UserService userService;
    private final DriverRepository driverRepository;
    private final DriverOnlineCacheService driverOnlineCacheService;

    @Override
    public void setDriverOnline(Long driverId, boolean isOnline) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));

        driver.setOnline(isOnline);
        driverRepository.save(driver);

        if (isOnline) {
            driverOnlineCacheService.driverOnline(driverId);
        } else {
            driverOnlineCacheService.driverOffline(driverId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Driver getDriverById(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机信息不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public Driver findNearestAvailableDriver() {
        return driverRepository.findFirstByOnlineTrue().stream()
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("暂无空闲司机"));
    }

    @Override
    public Driver createDriver(String name, String phone, String password,
                               String licenseNumber, String vehicleType, String vehiclePlate) {
        User user = userService.createUser(name, phone, password, UserRole.DRIVER);

        Driver driver = (Driver) user;
        driver.setLicenseNumber(licenseNumber);
        driver.setVehicleType(vehicleType);
        driver.setVehiclePlate(vehiclePlate);
        driver.setOnline(false);

        return driverRepository.save(driver);
    }

    @Override
    public void updateVehiclePlate(Long driverId, String vehiclePlate) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));
        driver.setVehiclePlate(vehiclePlate);
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getDriverInfo(Long userId) {
        Driver driver = driverRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));

        return UserDTO.builder()
                .userId(driver.getUserId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .role(driver.getRole().name())
                .rating(driver.getRating())
                .isOnline(driver.getOnline())
                .vehiclePlate(driver.getVehiclePlate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUserInfo(Long userId) {
        User user = userService.findUserById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        UserDTO.UserDTOBuilder builder = UserDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .rating(user.getRating());

        if (user instanceof Driver driver) {
            builder.isOnline(driver.getOnline())
                    .licenseNumber(driver.getLicenseNumber())
                    .vehicleType(driver.getVehicleType())
                    .vehiclePlate(driver.getVehiclePlate());
        }

        return builder.build();
    }

    @Override
    public UserDTO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userService.findUserById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));
        }

        if (user instanceof Driver driver) {
            if (request.getVehicleType() != null && !request.getVehicleType().isBlank()) {
                driver.setVehicleType(request.getVehicleType().trim());
            }
            if (request.getVehiclePlate() != null && !request.getVehiclePlate().isBlank()) {
                driver.setVehiclePlate(request.getVehiclePlate().trim());
            }
        }

        return getCurrentUserInfo(userId);
    }
}
