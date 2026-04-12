package org.example.Ride_Hailing_Platform;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.trip.Trip;
import org.example.Ride_Hailing_Platform.model.trip.TripStatus;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.model.user.UserRole;
import org.example.Ride_Hailing_Platform.repository.DriverRepository;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.PassengerRepository;
import org.example.Ride_Hailing_Platform.repository.TripRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.DriverService;
import org.example.Ride_Hailing_Platform.service.OrderService;
import org.example.Ride_Hailing_Platform.service.TripService;
import org.example.Ride_Hailing_Platform.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional // 关键：每个测试跑完自动回滚，不污染数据库
class RideHailingPlatformFullFlowTests {

    // ==================== 注入所有需要的 Service 和 Repository ====================
    @Autowired
    private UserService userService;
    @Autowired
    private DriverService driverService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TripService tripService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private PassengerRepository passengerRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TripRepository tripRepository;

    // ==================== 测试1：用户（乘客）注册 ====================
    @Test
    void testPassengerRegistration() {
        // 1. 调用注册
        User passenger = userService.createUser(
                "测试乘客小王",
                "13800000001",
                "123456",
                UserRole.PASSENGER
        );

        // 2. 断言验证
        Assertions.assertNotNull(passenger);
        Assertions.assertNotNull(passenger.getId());
        Assertions.assertEquals("测试乘客小王", passenger.getName());
        Assertions.assertEquals(UserRole.PASSENGER, passenger.getUserRole());

        // 3. 验证数据库真的存了
        Passenger savedPassenger = passengerRepository.findById(passenger.getId()).orElse(null);
        Assertions.assertNotNull(savedPassenger);
    }

    // ==================== 测试2：司机注册 & 上线 ====================
    @Test
    void testDriverRegistrationAndOnline() {
        // 1. 注册司机
        User driverUser = userService.createUser(
                "测试司机老李",
                "13900000001",
                "123456",
                UserRole.DRIVER
        );

        // 2. 假设你有 DriverService 来管理司机上线/下线
        // 如果这里报错，说明你还没实现 DriverService.setOnline()
        Driver driver = driverRepository.findById(driverUser.getId()).orElse(null);
        Assertions.assertNotNull(driver);

        driverService.setDriverOnline(driver.getId(), true); // 司机上线

        // 3. 验证司机状态是在线
        Driver onlineDriver = driverRepository.findById(driver.getId()).orElse(null);
        Assertions.assertTrue(onlineDriver.isOnline()); // 假设你有 isOnline() 字段
    }

    // ==================== 测试3：核心流程 - 乘客下单 → 司机接单 → 行程开始 → 到达目的地 ====================
    @Test
    void testFullOrderAndTripFlow() {
        // -------------------- 前置准备：创建乘客和在线司机 --------------------
        User passenger = userService.createUser("测试乘客", "13800000002", "123456", UserRole.PASSENGER);
        User driverUser = userService.createUser("测试司机", "13900000002", "123456", UserRole.DRIVER);
        Driver driver = driverRepository.findById(driverUser.getId()).orElse(null);
        driverService.setDriverOnline(driver.getId(), true);

        // -------------------- 步骤1：乘客创建订单 --------------------
        // 如果这里报错，说明你还没实现 OrderService.createOrder()
        Order order = orderService.createOrder(
                passenger.getId(),
                "起点：温州肯恩大学",
                "终点：温州南站",
                15.5 // 假设距离15.5公里
        );

        // 验证订单
        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(OrderStatus.WAITING, order.getStatus()); // 待接单
        Assertions.assertTrue(order.getPrice() > 0); // 动态定价生效，价格大于0

        // -------------------- 步骤2：系统派单 & 司机接单 --------------------
        // 如果这里报错，说明你还没实现 TripService 或 派单逻辑
        Trip trip = tripService.createTrip(order.getId(), driver.getId());
        Assertions.assertNotNull(trip);
        Assertions.assertNotNull(trip.getId());

        // 司机确认接单
        tripService.acceptTrip(trip.getId());

        // 验证订单状态变为已接单
        Order acceptedOrder = orderRepository.findById(order.getId()).orElse(null);
        Assertions.assertEquals(OrderStatus.ACCEPTED, acceptedOrder.getStatus());

        // 验证行程状态
        Trip acceptedTrip = tripRepository.findById(trip.getId()).orElse(null);
        Assertions.assertEquals(TripStatus.ACCEPTED, acceptedTrip.getStatus());

        // -------------------- 步骤3：司机到达起点，行程开始 --------------------
        tripService.startTrip(trip.getId());

        // 验证
        Trip ongoingTrip = tripRepository.findById(trip.getId()).orElse(null);
        Assertions.assertEquals(TripStatus.ONGOING, ongoingTrip.getStatus());

        Order ongoingOrder = orderRepository.findById(order.getId()).orElse(null);
        Assertions.assertEquals(OrderStatus.ONGOING, ongoingOrder.getStatus());

        // -------------------- 步骤4：到达目的地，结束行程 --------------------
        tripService.endTrip(trip.getId());

        // 最终验证
        Trip completedTrip = tripRepository.findById(trip.getId()).orElse(null);
        Assertions.assertEquals(TripStatus.COMPLETED, completedTrip.getStatus());

        Order completedOrder = orderRepository.findById(order.getId()).orElse(null);
        Assertions.assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
    }

    // ==================== 测试4：动态定价逻辑（单独验证） ====================
    @Test
    void testDynamicPricing() {
        // 这里可以直接测试你的 PriceService 或 OrderService 里的定价逻辑
        // 假设你有一个 calculatePrice 方法
        double basePrice = orderService.calculatePrice(5.0); // 5公里
        double peakPrice = orderService.calculatePrice(5.0, true); // 5公里 + 高峰时段

        Assertions.assertTrue(basePrice > 0);
        Assertions.assertTrue(peakPrice > basePrice); // 高峰应该更贵
    }
}