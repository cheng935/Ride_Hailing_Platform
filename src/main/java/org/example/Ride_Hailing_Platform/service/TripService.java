// src/main/java/org/example/Ride_Hailing_Platform/service/TripService.java
package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.trip.Location;
import org.example.Ride_Hailing_Platform.model.trip.Trip;
import org.example.Ride_Hailing_Platform.model.trip.TripStatus;
import org.example.Ride_Hailing_Platform.model.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 行程服务接口 - 定义行程业务契约
 */
public interface TripService {

    /**
     * 根据订单创建行程
     * @param orderId 订单ID
     * @return 创建的行程
     * @throws IllegalStateException 如果订单不存在或状态不允许
     */
    Trip createTripFromOrder(Long orderId);

    /**
     * 开始行程
     * @param tripId 行程ID
     * @param driverId 司机ID
     * @return 更新后的行程
     * @throws IllegalStateException 如果行程状态不允许开始
     */
    Trip startTrip(Long tripId, Long driverId);

    /**
     * 更新司机当前位置
     * @param tripId 行程ID
     * @param location 当前位置
     * @return 更新后的行程
     * @throws IllegalStateException 如果行程不存在或未开始
     */
    Trip updateDriverLocation(Long tripId, Location location);

    /**
     * 完成行程
     * @param tripId 行程ID
     * @param driverId 司机ID
     * @param actualDistance 实际距离
     * @param actualFare 实际费用
     * @param rating 评分
     * @param feedback 评价
     * @return 更新后的行程
     * @throws IllegalStateException 如果行程状态不允许完成
     */
    Trip completeTrip(Long tripId, Long driverId, Double actualDistance, Double actualFare,
                      Integer rating, String feedback);

    /**
     * 根据ID查找行程
     * @param tripId 行程ID
     * @return 行程对象（可选）
     */
    Optional<Trip> findTripById(Long tripId);

    /**
     * 获取用户的进行中行程
     * @param userId 用户ID
     * @param isPassenger 是否为乘客
     * @return 进行中行程列表
     */
    List<Trip> getActiveTrips(Long userId, boolean isPassenger);

    /**
     * 获取司机的统计信息
     * @param driverId 司机ID
     * @return 统计信息
     */
    DriverTripStats getDriverTripStats(Long driverId);

    /**
     * 司机统计信息类
     */
    record DriverTripStats(
            long completedTrips,
            double averageRating,
            double totalEarnings
    ) {}
}
