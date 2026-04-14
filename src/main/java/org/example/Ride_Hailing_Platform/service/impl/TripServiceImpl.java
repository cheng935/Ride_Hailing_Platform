// src/main/java/org/example/Ride_Hailing_Platform/service/impl/TripServiceImpl.java
package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.trip.*;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.TripRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @Override
    public Trip createTripFromOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!OrderStatus.ACCEPTED.equals(order.getStatus())) {
            throw new IllegalStateException("只有已接单的订单才能创建行程");
        }

        // 检查是否已存在行程
        if (tripRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("该订单已有对应的行程");
        }

        Trip trip = new Trip();
        trip.setOrder(order);

        // 设置位置信息
        trip.setPickupLocation(new Location(
                null, null, order.getPickupLocation() // 实际由前端传真实坐标
        ));

        trip.setDestinationLocation(new Location(
                null, null, order.getDestination()
        ));

        trip.setStatus(TripStatus.NOT_STARTED);
        trip.setCreateTime(LocalDateTime.now()); // 补充创建时间

        return tripRepository.save(trip);
    }

    @Override
    public Trip startTrip(Long tripId, Long driverId) {
        // 1. 校验行程存在
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在（tripId: " + tripId + "）"));

        // 2. 校验订单关联
        Order order = trip.getOrder();
        if (order == null) {
            throw new IllegalArgumentException("行程未关联任何订单（tripId: " + tripId + "）");
        }

        // 3. 单表继承核心修改：直接从UserRepository查Driver（无需查DriverRepository）
        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在（userId: " + driverId + "）"));

        // 校验是司机角色
        if (!(driverUser instanceof Driver)) {//instanceof 是Java原生的类型判断段运算符，判断对象driverUser是不是Driver的实例
            throw new IllegalArgumentException("只有司机可以开始行程（userId: " + driverId + "）");
        }
        Driver driver = (Driver)driverUser;

        // 4. 校验订单的司机是当前操作的司机（单表继承：用userId对比，无driverId）
        Driver orderDriver = order.getDriver();
        if (!orderDriver.getUserId().equals(driverId)) {
            throw new IllegalArgumentException("无权操作此行程：当前司机（driverId: " + driverId + "）不是订单绑定的司机（driverId: " + orderDriver.getUserId() + "）");
        }

        // 5. 校验行程状态
        if (!TripStatus.NOT_STARTED.equals(trip.getStatus())) {
            throw new IllegalStateException("行程状态不允许开始（当前状态：" + trip.getStatus() + "）");
        }

        // 6. 校验司机在线
        if (!driver.getIsOnline()) {
            throw new IllegalStateException("司机未上线，无法开始行程（driverId: " + driverId + "）");
        }

        // 7. 更新行程状态
        trip.startTrip();
        trip.setCurrentLocation(trip.getPickupLocation());
        trip.setUpdateTime(LocalDateTime.now());

        // 8. 同步更新订单状态为IN_PROGRESS（业务闭环）
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);

        return tripRepository.save(trip);
    }

    @Override
    public Trip updateDriverLocation(Long tripId, Location location) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在"));

        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new IllegalStateException("只有进行中的行程才能更新位置");
        }

        // 优化：位置非空校验
        if (location == null || location.getAddress() == null || location.getAddress().isBlank()) {
            throw new IllegalArgumentException("位置信息不能为空");
        }

        trip.setCurrentLocation(location);
        return tripRepository.save(trip);
    }

    @Override
    public Trip completeTrip(Long tripId, Long driverId, Double actualDistance, Double actualFare, Integer rating, String feedback) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在"));

        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在"));
        if (!(driverUser instanceof Driver)) {
            throw new IllegalArgumentException("该用户不是司机（userId: " + driverId + "）");
        }
        Driver driver = (Driver) driverUser;

        // 验证司机是否是该订单的司机
        Driver orderDriver = trip.getOrder().getDriver();
        if (!orderDriver.getUserId().equals(driverId)) {
            throw new IllegalArgumentException("无权操作此行程");
        }

        // 校验行程状态
        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new IllegalStateException("只有进行中的行程才能完成");
        }

        // 验证参数
        if (actualDistance == null || actualDistance <= 0) {
            throw new IllegalArgumentException("实际距离必须大于0");
        }

        if (actualFare == null || actualFare <= 0) {
            throw new IllegalArgumentException("实际费用必须大于0");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }

        trip.completeTrip(actualDistance, actualFare, rating, feedback);
        trip.setUpdateTime(LocalDateTime.now());

        // 更新订单状态
        Order order = trip.getOrder();
        order.setStatus(OrderStatus.COMPLETED);
        order.setActualFare(actualFare);// 补充订单实际费用
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);

        return tripRepository.save(trip);
    }

    @Override
    public Optional<Trip> findTripById(Long tripId) {
        return tripRepository.findById(tripId);
    }

    @Override
    public List<Trip> getActiveTrips(Long userId, boolean isPassenger) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在（userId: " + userId + "）"));

        List<TripStatus> activeStatuses = List.of(TripStatus.NOT_STARTED, TripStatus.IN_PROGRESS);

        if (isPassenger) {
            if (!(user instanceof Passenger)) {
                throw new IllegalArgumentException("该用户不是乘客（userId: " + userId + "）");
            }
            Passenger passenger = (Passenger) user;
            return tripRepository.findByOrderPassengerAndStatusIn(passenger, activeStatuses);
        } else {
            // 校验是司机
            if (!(user instanceof Driver)) {
                throw new IllegalArgumentException("该用户不是司机（userId: " + userId + "）");
            }
            Driver driver = (Driver) user;
            return tripRepository.findByOrderDriverAndStatusIn(driver, activeStatuses);
        }
    }

    @Override
    public DriverTripStats getDriverTripStats(Long driverId) {
        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在（userId: " + driverId + "）"));

        if (!(driverUser instanceof Driver)) {
            throw new IllegalArgumentException("该用户不是司机（userId: " + driverId + "）");
        }
        Driver driver = (Driver) driverUser;

        long completedTrips = tripRepository.countByOrderDriverAndStatus(driver, TripStatus.COMPLETED);

        // 计算平均评分
        List<Trip> completedTripsList = tripRepository.findByOrderDriverAndStatus(driver, TripStatus.COMPLETED);
        double averageRating = completedTripsList.stream()
                .filter(trip -> trip.getRating() != null)
                .mapToInt(Trip::getRating)
                .average()
                .orElse(0.0);
        // 保留2位小数
        averageRating = Math.round(averageRating * 100.0) / 100.0;

        // 计算总收入
        double totalEarnings = completedTripsList.stream()
                .filter(trip -> trip.getActualFare() != null)
                .mapToDouble(Trip::getActualFare)
                .sum();
        totalEarnings = Math.round(totalEarnings * 100.0) / 100.0;

        return new DriverTripStats(completedTrips, averageRating, totalEarnings);
    }
}