// src/main/java/org/example/Ride_Hailing_Platform/service/impl/TripServiceImpl.java
package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.trip.*;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
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
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

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
                39.9042, 116.4074, order.getPickupLocation() // 默认北京坐标，实际应获取真实坐标
        ));

        trip.setDestinationLocation(new Location(
                39.9043, 116.4075, order.getDestination()
        ));

        trip.setStatus(TripStatus.NOT_STARTED);

        return tripRepository.save(trip);
    }

    @Override
    public Trip startTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在"));

        if (!UserRole.DRIVER.equals(driver.getRole())) {
            throw new IllegalArgumentException("只有司机可以开始行程");
        }

        // 验证司机是否是该订单的司机
        if (!trip.getOrder().getDriver().getUserId().equals(driverId)) {
            throw new IllegalArgumentException("无权操作此行程");
        }

        if (!TripStatus.NOT_STARTED.equals(trip.getStatus())) {
            throw new IllegalStateException("行程状态不允许开始");
        }

        trip.startTrip();
        trip.setCurrentLocation(trip.getPickupLocation());

        return tripRepository.save(trip);
    }

    @Override
    public Trip updateDriverLocation(Long tripId, Location location) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在"));

        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new IllegalStateException("只有进行中的行程才能更新位置");
        }

        trip.setCurrentLocation(location);
        trip.setUpdateTime(LocalDateTime.now());

        return tripRepository.save(trip);
    }

    @Override
    public Trip completeTrip(Long tripId, Long driverId, Double actualDistance, Double actualFare,
                             Integer rating, String feedback) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("行程不存在"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在"));

        if (!UserRole.DRIVER.equals(driver.getRole())) {
            throw new IllegalArgumentException("只有司机可以完成行程");
        }

        // 验证司机是否是该订单的司机
        if (!trip.getOrder().getDriver().getUserId().equals(driverId)) {
            throw new IllegalArgumentException("无权操作此行程");
        }

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

        // 更新订单状态
        Order order = trip.getOrder();
        order.setStatus(OrderStatus.COMPLETED);
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
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        List<TripStatus> activeStatuses = List.of(TripStatus.NOT_STARTED, TripStatus.IN_PROGRESS);

        if (isPassenger) {
            return tripRepository.findByOrderPassengerAndStatusIn(user, activeStatuses);
        } else {
            return tripRepository.findByOrderDriverAndStatusIn(user, activeStatuses);
        }
    }

    @Override
    public DriverTripStats getDriverTripStats(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在"));

        if (!UserRole.DRIVER.equals(driver.getRole())) {
            throw new IllegalArgumentException("只有司机可以获取统计信息");
        }

        long completedTrips = tripRepository.countByOrderDriverAndStatus(driver, TripStatus.COMPLETED);

        // 计算平均评分
        List<Trip> completedTripsList = tripRepository.findByOrderDriverAndStatus(driver, TripStatus.COMPLETED);
        double averageRating = completedTripsList.stream()
                .filter(trip -> trip.getRating() != null)
                .mapToInt(Trip::getRating)
                .average()
                .orElse(0.0);

        // 计算总收入
        double totalEarnings = completedTripsList.stream()
                .filter(trip -> trip.getActualFare() != null)
                .mapToDouble(Trip::getActualFare)
                .sum();

        return new DriverTripStats(completedTrips, averageRating, totalEarnings);
    }
}