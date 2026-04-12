package org.example.Ride_Hailing_Platform.service.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.service.DriverService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

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

    // 简单版：找第一个空闲司机（后期可加距离算法）
    @Override
    public Driver findNearestAvailableDriver() {
        return driverRepository.findFirstByIsOnlineTrue()
                .orElseThrow(() -> new RuntimeException("暂无空闲司机"));
    }

    @Override
    public Driver getById(Long driverId) {
        return driverRepository.findById(driverId).orElse(null);
    }

}
