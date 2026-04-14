// src/main/java/org/example/Ride_Hailing_Platform/service/impl/OrderServiceImpl.java
package org.example.Ride_Hailing_Platform.service.impl;

import org.example.Ride_Hailing_Platform.model.order.*;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.TripRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.service.PassengerService;
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
    private final DriverRepository driverRepository;

    @Override
    public Order createOrder(Long passengerId, String pickupLocation, String destination, OrderType type, Boolean isCongestion) {
        Passenger passenger = userRepository.findById(passengerId)
                .filter(u -> u instanceof Passenger)
                .map(u -> (Passenger) u)
                .orElseThrow(() -> new IllegalArgumentException("乘客不存在或用户类型错误"));

        if (pickupLocation == null || destination == null || pickupLocation.isBlank() || destination.isBlank() || pickupLocation.equals(destination) ) {
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
        order.setIsCongestion(isCongestion);//是否高峰期
        order.setCreateTime(LocalDateTime.now()); // 补充创建时间。
        order.setUpdateTime(LocalDateTime.now()); // 补充更新时间

        // 简单的预估费用计算（实际应使用地图API）
        double distance = calculateDistance(pickupLocation, destination);
        order.setDistance(distance);
        order.setEstimatedFare(calculateEstimatedFare(distance, type, isCongestion));

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

        //删掉if，自从用户类改为单表继承,腰也不酸了，背也不疼了，
        //单表继承：直接查Driver（子类），无需查User再关联
        Driver driver = userRepository.findById(driverId)
                .filter(u -> u instanceof Driver)
                .map(u -> (Driver) u)
                .orElseThrow(() -> new IllegalArgumentException("司机不存在或用户类型错误"));


        // 校验订单状态
        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("仅待接单状态的订单可被接受");
        }

        // 检查司机是否在线
        if (Boolean.FALSE.equals(driver.getIsOnline())) {
            throw new IllegalStateException("司机未上线，无法接单");
        }

        // 检查司机是否有未完成的订单
        if (orderRepository.existsByDriverAndStatusIn(driver,
                List.of(OrderStatus.ACCEPTED, OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS))) {
            throw new IllegalStateException("您有未完成的订单，请先完成");
        }

        order.setDriver(driver);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setUpdateTime(LocalDateTime.now());
        order.setAcceptTime(LocalDateTime.now()); // 补充接单时间

        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 检查用户是否有权限取消此订单
        boolean isPassenger = order.getPassenger().getUserId().equals(userId);//乘客是不是该订单的乘客
        boolean isDriver = order.getDriver() != null && order.getDriver().getUserId().equals(userId);//司机是不是该订单的司机

        if (!isPassenger && !isDriver) {
            throw new IllegalArgumentException("无权取消此订单");
        }

        // 检查订单状态是否允许取消
        if (!order.canBeCancelled()) {
            throw new IllegalStateException("当前订单状态（" + order.getStatus() + "）不允许取消");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        order.setCancelTime(LocalDateTime.now()); // 补充取消时间
        order.setCancelReason(isPassenger ? "乘客主动取消" : "司机主动取消"); // 补充取消原因

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
            // 乘客：强转为Passenger（单表继承）
            Passenger passenger = (Passenger) user;
            //用 passenger 查询订单
            return orderRepository.findByPassengerAndStatusIn(passenger, activeStatuses);
        } else {
            // 司机：强转为Driver（单表继承，无需查driverRepository）
            Driver driver = (Driver) user;
            //用 Driver 查询订单
            return orderRepository.findByDriverAndStatusIn(driver, activeStatuses);
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
            Passenger passenger = (Passenger) user;
            return orderRepository.findByPassengerAndStatusIn(passenger, historyStatuses);
        } else {
            Driver driver = (Driver) user;
            return orderRepository.findByDriverAndStatusIn(driver, historyStatuses);
        }
    }

    @Override
    public Order requestRide(Passenger passenger, String pickupLocation, String destination, Boolean isCongestion) {
        // 单表继承：Passenger本身就是User，直接取userId
        if (passenger == null || passenger.getUserId() == null) {
            throw new IllegalArgumentException("乘客信息不能为空");
        }
        // 调用createOrder创建订单（标准化）
        return createOrder(passenger.getUserId(), pickupLocation, destination, OrderType.STANDARD, isCongestion);
    }


    // 辅助方法 - 简单的距离和费用计算
    private double calculateDistance(String pickup, String destination) {
        // 实际应用中应使用地图API
        return Math.abs(pickup.hashCode() - destination.hashCode()) % 100 / 10.0;
    }

    public Double calculateEstimatedFare(double distance, OrderType type, Boolean isCongestion) {
        double baseFare = 10.0; // 起步价
        double perKmRate = switch (type) {
            case STANDARD -> 2.0;
            case PREMIUM -> 3.5;
            case EXPRESS -> 2.5;
        };

        double fare = baseFare + (distance * perKmRate);
        //高峰期涨价0.2倍
        if (isCongestion){
            fare *= 1.2;
        }

        return fare;
    }

    @Override
    public List<Order> getOrdersByPassengerId(Long passengerId) {
        List<Order> order = orderRepository.findRecentOrdersByPassenger(passengerId);
        return order;
    }


}