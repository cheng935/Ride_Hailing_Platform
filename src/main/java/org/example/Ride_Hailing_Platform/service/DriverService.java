package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.user.Driver;

/**
 * 司机服务接口
 */
public interface DriverService{

    /**
     * 设置司机上下线（能否接单）
     * @param driverId  司机ID
     * @param isOnline  上线状态 (true:online)
     */
    void setDriverOnline(Long driverId, boolean isOnline);

    /**
     * 通过ID查询司机是否存在且在线
     * @param driverId  司机ID
     * @return          对应ID的司机对象
     */
    Driver getById(Long driverId);


    /**
     * 查询附近空闲司机(派单用)
     * @return  可接单的司机
     */
    Driver findNearestAvailableDriver();
}
