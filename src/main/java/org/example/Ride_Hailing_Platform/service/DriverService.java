package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.user.Driver;

/**
 * 司机服务接口
 */
public interface DriverService {

    /**
     * 设置司机上下线（能否接单）
     * @param driverId  司机ID
     * @param isOnline  上线状态 (true:online)
     */
    void setDriverOnline(Long driverId, boolean isOnline);

    /**
     * 通过ID查询司机
     * @param driverId  司机ID
     * @return          对应ID的司机对象
     */
    Driver getDriverById(Long driverId);


    /**
     * 查询附近空闲司机(派单用)
     * @return  可接单的司机
     */
    Driver findNearestAvailableDriver();

    /**
     * 司机专属：创建司机--复用 UserService 创建用户后，补充司机专属信息
     * @param name
     * @param phone
     * @param password
     * @param licenseNumber
     * @param vehicleType
     * @param vehiclePlate
     * @return
     */
    Driver createDriver(String name, String phone, String password,
                        String licenseNumber, String vehicleType, String vehiclePlate);
}
