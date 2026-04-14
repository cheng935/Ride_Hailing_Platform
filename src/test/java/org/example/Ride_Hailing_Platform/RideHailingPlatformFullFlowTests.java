package org.example.Ride_Hailing_Platform;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.order.OrderType;
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
@Transactional // 每个测试跑完自动回滚，不污染数据库
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
        User passengerUser = userService.createUser(
                "测试乘客小王",
                "13800000001",
                "123456",
                UserRole.PASSENGER
        );

        // 2. 断言验证（单表继承：User本身就是Passenger）
        Assertions.assertNotNull(passengerUser);
        Assertions.assertNotNull(passengerUser.getUserId());
        Assertions.assertEquals("测试乘客小王", passengerUser.getName());
        Assertions.assertEquals(UserRole.PASSENGER, passengerUser.getRole());
        Assertions.assertTrue(passengerUser instanceof Passenger); // 验证是乘客子类

        // 3. 验证数据库存储（单表继承：通过user_id查Passenger）
        Passenger savedPassenger = passengerRepository.findByUserId(passengerUser.getUserId());
        Assertions.assertNotNull(savedPassenger);
        Assertions.assertEquals(passengerUser.getUserId(), savedPassenger.getUserId()); // 复用userId
    }

    // ==================== 测试2：司机注册 & 上线 ====================
    @Test
    void testDriverRegistrationAndOnline() {
        // 1. 注册司机（单表继承：createUser返回的User就是Driver子类）
        User driverUser = userService.createUser(
                "测试司机老李",
                "13900000001",
                "123456",
                UserRole.DRIVER
        );
        Assertions.assertTrue(driverUser instanceof Driver); // 验证是司机子类

        // 2. 强转为Driver（单表继承核心：无需查driverId，用userId即可）
        Driver driver = (Driver) driverUser;
        driverService.setDriverOnline(driver.getUserId(), true); // 修复：传userId而非driverId

        // 3. 验证司机状态是在线（重新查询验证）
        Driver onlineDriver = driverRepository.findByUserId(driver.getUserId()).orElseThrow();
        Assertions.assertNotNull(onlineDriver);
        Assertions.assertTrue(onlineDriver.getIsOnline());
        Assertions.assertEquals(driver.getUserId(), onlineDriver.getUserId()); // 复用userId
    }

    // ==================== 测试3：核心流程 - 乘客下单 → 司机接单 → 行程开始 → 到达目的地 ====================
    @Test
    void testFullOrderAndTripFlow() {
        // -------------------- 前置准备：创建乘客和在线司机 --------------------
        // 1. 创建乘客
        User passengerUser = userService.createUser("测试乘客", "13800000002", "123456", UserRole.PASSENGER);
        Passenger passenger = (Passenger) passengerUser; // 强转为Passenger

        // 2. 创建司机并上线（单表继承核心修复）
        User driverUser = userService.createUser("测试司机", "13900000002", "123456", UserRole.DRIVER);
        Driver driver = (Driver) driverUser; // 强转为Driver
        driverService.setDriverOnline(driver.getUserId(), true); // 传userId上线

        // -------------------- 步骤1：乘客创建订单 --------------------
        Order order = orderService.createOrder(
                passenger.getUserId(),
                "起点：温州肯恩大学",
                "终点：温州南站",
                OrderType.STANDARD,
                false
        );

        // 验证订单基础信息
        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getOrderId());
        Assertions.assertEquals(OrderStatus.PENDING, order.getStatus());
        Assertions.assertTrue(order.getEstimatedFare() > 0);
        Assertions.assertEquals(passenger, order.getPassenger()); // 订单关联正确乘客

        // -------------------- 步骤2：司机接单 → 创建行程 --------------------
        // 1. 更新订单状态为已接单 + 绑定司机（单表继承：直接绑定Driver对象）
        order.setStatus(OrderStatus.ACCEPTED);
        order.setDriver(driver); // 绑定Driver（而非User）
        orderRepository.save(order);

        // 2. 创建行程（验证：只有已接单的订单能创建行程）
        Trip trip = tripService.createTripFromOrder(order.getOrderId());
        Assertions.assertNotNull(trip);
        Assertions.assertNotNull(trip.getTripId());
        Assertions.assertEquals(TripStatus.NOT_STARTED, trip.getStatus());
        Assertions.assertEquals(order, trip.getOrder()); // 行程关联正确订单

        // 验证订单状态
        Order acceptedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.ACCEPTED, acceptedOrder.getStatus());
        Assertions.assertEquals(driver, acceptedOrder.getDriver()); // 订单绑定正确司机

        // -------------------- 步骤3：司机开始行程 --------------------
        // 核心修复：传userId（单表继承下driverId=userId）
        Trip startedTrip = tripService.startTrip(trip.getTripId(), driver.getUserId());

        // 验证行程状态
        Assertions.assertEquals(TripStatus.IN_PROGRESS, startedTrip.getStatus());
        Assertions.assertNotNull(startedTrip.getStartTime()); // 开始时间已设置

        // 验证订单同步更新
        Order ongoingOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.IN_PROGRESS, ongoingOrder.getStatus());

        // -------------------- 步骤4：完成行程 --------------------
        Double actualDistance = order.getDistance() != null ? order.getDistance() : 5.0; // 兜底默认值
        Double actualFare = order.getEstimatedFare() != null ? order.getEstimatedFare() : 12.5;
        Trip completedTrip = tripService.completeTrip(
                trip.getTripId(),
                driver.getUserId(),      // 传userId而非driverId
                actualDistance,
                actualFare,
                5,
                "服务非常好，司机很专业！"
        );

        // 最终验证
        Assertions.assertEquals(TripStatus.COMPLETED, completedTrip.getStatus());
        Assertions.assertNotNull(completedTrip.getEndTime()); // 结束时间已设置
        Assertions.assertEquals(actualDistance, completedTrip.getActualDistance());
        Assertions.assertEquals(actualFare, completedTrip.getActualFare());
        Assertions.assertEquals(5, completedTrip.getRating()); // 评分正确

        // 验证订单最终状态
        Order completedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        Assertions.assertEquals(actualFare, completedOrder.getActualFare()); // 订单同步实际费用
    }

    // ==================== 测试4：动态定价逻辑（单独验证） ====================
    @Test
    void testDynamicPricing() {
        // 测试基础定价和高峰定价
        double basePrice = orderService.calculateEstimatedFare(5.0, OrderType.STANDARD, false);
        double peakPrice = orderService.calculateEstimatedFare(5.0, OrderType.STANDARD, true);

        // 断言：价格大于0，高峰价高于基础价
        Assertions.assertTrue(basePrice > 0, "基础定价必须大于0");
        Assertions.assertTrue(peakPrice > basePrice, "高峰时段定价必须高于基础定价");
        Assertions.assertTrue(peakPrice > 0, "高峰定价必须大于0");
    }
}