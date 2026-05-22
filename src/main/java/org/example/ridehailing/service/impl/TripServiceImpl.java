package org.example.ridehailing.service.impl;

import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.order.OrderStatus;
import org.example.ridehailing.model.trip.*;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.Passenger;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.repository.DriverRepository;
import org.example.ridehailing.repository.OrderRepository;
import org.example.ridehailing.repository.TripRepository;
import org.example.ridehailing.repository.UserRepository;
import org.example.ridehailing.service.TripService;
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
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (!OrderStatus.ACCEPTED.equals(order.getStatus())) {
            throw BusinessException.badRequest("只有已接单的订单才能创建行程");
        }

        if (tripRepository.findByOrderId(orderId).isPresent()) {
            throw BusinessException.badRequest("该订单已有对应的行程");
        }

        Trip trip = new Trip();
        trip.setOrder(order);
        trip.setPickupLocation(new Location(null, null, order.getPickupLocation()));
        trip.setDestinationLocation(new Location(null, null, order.getDestination()));
        trip.setStatus(TripStatus.NOT_STARTED);
        trip.setCreateTime(LocalDateTime.now());

        return tripRepository.save(trip);
    }

    @Override
    public Trip startTrip(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> BusinessException.notFound("行程不存在"));

        Order order = trip.getOrder();
        if (order == null) {
            throw BusinessException.badRequest("行程未关联任何订单");
        }

        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));

        if (!(driverUser instanceof Driver driver)) {
            throw BusinessException.badRequest("只有司机可以开始行程");
        }

        Driver orderDriver = order.getDriver();
        if (!orderDriver.getUserId().equals(driverId)) {
            throw BusinessException.forbidden("无权操作此行程");
        }

        if (!TripStatus.NOT_STARTED.equals(trip.getStatus())) {
            throw BusinessException.badRequest("行程状态不允许开始");
        }

        if (!driver.getOnline()) {
            throw BusinessException.badRequest("司机未上线，无法开始行程");
        }

        trip.startTrip();
        trip.setCurrentLocation(trip.getPickupLocation());
        trip.setUpdateTime(LocalDateTime.now());

        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);

        return tripRepository.save(trip);
    }

    @Override
    public Trip updateDriverLocation(Long tripId, Location location) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> BusinessException.notFound("行程不存在"));

        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw BusinessException.badRequest("只有进行中的行程才能更新位置");
        }

        if (location == null || location.getAddress() == null || location.getAddress().isBlank()) {
            throw BusinessException.badRequest("位置信息不能为空");
        }

        trip.setCurrentLocation(location);
        return tripRepository.save(trip);
    }

    @Override
    public Trip completeTrip(Long tripId, Long driverId, Double actualDistance, Double actualFare, Integer rating, String feedback) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> BusinessException.notFound("行程不存在"));

        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));
        if (!(driverUser instanceof Driver)) {
            throw BusinessException.badRequest("该用户不是司机");
        }

        Driver orderDriver = trip.getOrder().getDriver();
        if (!orderDriver.getUserId().equals(driverId)) {
            throw BusinessException.forbidden("无权操作此行程");
        }

        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw BusinessException.badRequest("只有进行中的行程才能完成");
        }

        if (actualDistance == null || actualDistance <= 0) {
            throw BusinessException.badRequest("实际距离必须大于0");
        }

        if (actualFare == null || actualFare <= 0) {
            throw BusinessException.badRequest("实际费用必须大于0");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw BusinessException.badRequest("评分必须在1-5之间");
        }

        trip.completeTrip(actualDistance, actualFare, rating, feedback);
        trip.setUpdateTime(LocalDateTime.now());

        Order order = trip.getOrder();
        order.setStatus(OrderStatus.COMPLETED);
        order.setActualFare(actualFare);
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
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        List<TripStatus> activeStatuses = List.of(TripStatus.NOT_STARTED, TripStatus.IN_PROGRESS);

        if (isPassenger) {
            if (!(user instanceof Passenger)) {
                throw BusinessException.badRequest("该用户不是乘客");
            }
            Passenger passenger = (Passenger) user;
            return tripRepository.findByOrderPassengerAndStatusIn(passenger, activeStatuses);
        } else {
            if (!(user instanceof Driver)) {
                throw BusinessException.badRequest("该用户不是司机");
            }
            Driver driver = (Driver) user;
            return tripRepository.findByOrderDriverAndStatusIn(driver, activeStatuses);
        }
    }

    @Override
    public DriverTripStats getDriverTripStats(Long driverId) {
        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("司机不存在"));

        if (!(driverUser instanceof Driver driver)) {
            throw BusinessException.badRequest("该用户不是司机");
        }

        long completedTrips = tripRepository.countByOrderDriverAndStatus(driver, TripStatus.COMPLETED);

        List<Trip> completedTripsList = tripRepository.findByOrderDriverAndStatus(driver, TripStatus.COMPLETED);
        double averageRating = completedTripsList.stream()
                .filter(trip -> trip.getRating() != null)
                .mapToInt(Trip::getRating)
                .average()
                .orElse(0.0);
        averageRating = Math.round(averageRating * 100.0) / 100.0;

        double totalEarnings = completedTripsList.stream()
                .filter(trip -> trip.getActualFare() != null)
                .mapToDouble(Trip::getActualFare)
                .sum();
        totalEarnings = Math.round(totalEarnings * 100.0) / 100.0;

        return new DriverTripStats(completedTrips, averageRating, totalEarnings);
    }
}
