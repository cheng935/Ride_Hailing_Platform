// src/main/java/org/example/Ride_Hailing_Platform/service/impl/OrderServiceImpl.java
package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.model.order.*;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.TripRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Override
    public Order createOrder(Long passengerId, String pickupLocation, String destination, OrderType type) {
        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("乘客不存在"));

        if (!UserRole.PASSENGER.equals(passenger.getRole())) {
            throw new IllegalArgumentException("只有乘客可以创建订单");
        }

        if (pickupLocation == null || destination == null || pickupLocation.equals(destination)) {
            throw new IllegalArgumentException("上车地点和目的地不能为空且不能相同");
        }

        // 检查是否有未完成的订单
        if (orderRepository.existsByPassengerAndStatusIn(passenger,
                List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS))) {
            throw new IllegalStateException("您有未完成的订单，请先完成或取消");
        }

        Order order = new Order();
        order.setPassenger(passenger);
        order.setPickupLocation(pickupLocation);
        order.setDestination(destination);
        order.setType(type);
        order.setStatus(OrderStatus.PENDING);

        // 简单的预估费用计算（实际应使用地图API）
        double distance = calculateDistance(pickupLocation, destination);
        order.setDistance(distance);
        order.setEstimatedFare(calculateEstimatedFare(distance, type));

        return orderRepository.save(order);
    }

    @Override
    public List<Order> getPendingOrders() {
        // 查询所有待处理订单（状态为 NEW 或 PENDING）
        return orderRepository.findByStatusIn(
                List.of(OrderStatus.PENDING)
        );
    }

    @Override
    public Order acceptOrder(Long orderId, Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在"));

        if (!UserRole.DRIVER.equals(driver.getRole())) {
            throw new IllegalArgumentException("只有司机可以接单");
        }

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("订单状态不允许接单");
        }

        // 检查司机是否有未完成的订单
        if (orderRepository.existsByDriverAndStatusIn(driver,
                List.of(OrderStatus.ACCEPTED, OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS))) {
            throw new IllegalStateException("您有未完成的订单，请先完成");
        }

        order.setDriver(driver);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setUpdateTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 检查用户是否有权限取消此订单
        boolean isPassenger = order.getPassenger().getUserId().equals(userId);
        boolean isDriver = order.getDriver() != null && order.getDriver().getUserId().equals(userId);

        if (!isPassenger && !isDriver) {
            throw new IllegalArgumentException("无权取消此订单");
        }

        // 检查订单状态是否允许取消
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("订单状态不允许取消");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> getActiveOrders(Long userId, boolean isPassenger) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING,
                OrderStatus.ACCEPTED,
                OrderStatus.PICKING_UP,
                OrderStatus.IN_PROGRESS
        );

        if (isPassenger) {
            return orderRepository.findByPassengerAndStatusIn(user, activeStatuses);
        } else {
            return orderRepository.findByDriverAndStatusIn(user, activeStatuses);
        }
    }

    @Override
    public List<Order> getHistoryOrders(Long userId, boolean isPassenger) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        List<OrderStatus> historyStatuses = List.of(
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED,
                OrderStatus.REJECTED
        );

        if (isPassenger) {
            return orderRepository.findByPassengerAndStatusIn(user, historyStatuses);
        } else {
            return orderRepository.findByDriverAndStatusIn(user, historyStatuses);
        }
    }

    @Override
    public Order requestRide(Passenger passenger, String pickupLocation, String destination) {
        if (passenger == null || passenger.getUser() == null) {
            throw new IllegalArgumentException("乘客信息不能为空");
        }

        Long passengerId = passenger.getUser().getUserId();
        return createOrder(passengerId, pickupLocation, destination, OrderType.STANDARD);
    }


    // 辅助方法 - 简单的距离和费用计算
    private double calculateDistance(String pickup, String destination) {
        // 实际应用中应使用地图API
        return Math.abs(pickup.hashCode() - destination.hashCode()) % 100 / 10.0;
    }

    private double calculateEstimatedFare(double distance, OrderType type) {
        double baseFare = 10.0; // 起步价
        double perKmRate = switch (type) {
            case STANDARD -> 2.0;
            case PREMIUM -> 3.5;
            case EXPRESS -> 2.5;
        };

        return baseFare + (distance * perKmRate);
    }


}