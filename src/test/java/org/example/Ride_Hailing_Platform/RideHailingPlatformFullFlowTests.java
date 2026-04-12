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
        Assertions.assertNotNull(passenger.getUserId());
        Assertions.assertEquals("测试乘客小王", passenger.getName());
        Assertions.assertEquals(UserRole.PASSENGER, passenger.getRole());

        // 3. 验证数据库真的存了
        //Passenger savedPassenger = passengerRepository.findById(passenger.getUserId()).orElse(null);
        //Assertions.assertNotNull(savedPassenger);

        // 3. 【修复】正确通过 userId 查询 passenger，不再用 userId 当 passengerId
        Passenger savedPassenger = passengerRepository.findByUserUserId(passenger.getUserId());
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

        // 2. 假设有 DriverService 来管理司机上线/下线
        // 如果这里报错，说明还没实现 DriverService.setOnline()
        //Driver driver = driverRepository.findById(driverUser.getUserId()).orElse(null);
        //Assertions.assertNotNull(driver);

        // 【修复】这里必须用正确方式获取 driver，否则 driver = null
        Driver driver = driverRepository.findByUserUserId(driverUser.getUserId());
        driverService.setDriverOnline(driver.getDriverId(), true);

        driverService.setDriverOnline(driver.getDriverId(), true); // 司机上线

        // 3. 验证司机状态是在线
        Driver onlineDriver = driverRepository.findById(driver.getDriverId()).orElse(null);
        Assertions.assertTrue(onlineDriver.getIsOnline()); // 假设你有 isOnline() 字段
    }

    // ==================== 测试3：核心流程 - 乘客下单 → 司机接单 → 行程开始 → 到达目的地 ====================
    @Test
    void testFullOrderAndTripFlow() {
        // -------------------- 前置准备：创建乘客和在线司机 --------------------
        User passenger = userService.createUser("测试乘客", "13800000002", "123456", UserRole.PASSENGER);
        User driverUser = userService.createUser("测试司机", "13900000002", "123456", UserRole.DRIVER);
        //Driver driver = driverRepository.findById(driverUser.getUserId()).orElse(null);
        //driverService.setDriverOnline(driver.getDriverId(), true);

        // 【修复】这里必须用正确方式获取 driver，否则 driver = null
        Driver driver = driverRepository.findByUserUserId(driverUser.getUserId());
        driverService.setDriverOnline(driver.getDriverId(), true);

        // -------------------- 步骤1：乘客创建订单 --------------------
        // 如果这里报错，说明你还没实现 OrderService.createOrder()
        Order order = orderService.createOrder(
                passenger.getUserId(),
                "起点：温州肯恩大学",
                "终点：温州南站",
                OrderType.STANDARD,
                false
        );// 当前算法尚未完善，距离使用Hashcode计算而来，无实际意义

        // 验证订单
        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getOrderId());
        Assertions.assertEquals(OrderStatus.PENDING, order.getStatus()); // 待接单
        Assertions.assertTrue(order.getEstimatedFare() > 0); // 动态定价生效，价格大于0

        // -------------------- 步骤2：系统派单 & 司机接单 --------------------
        // 如果这里报错，说明你还没实现 TripService 或 派单逻辑
        //必须先接单到ACCEPTED状态才能创建行程
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);

        Trip trip = tripService.createTripFromOrder(order.getOrderId());
        Assertions.assertNotNull(trip);
        Assertions.assertNotNull(trip.getTripId());

        /*
        司机确认接单(已弃用，因为确认接单是OrderService操作，接取Order必然接取trip，此处多此一举)
        不删除原因: 一般打车软件会在接单后让司机评估行程，期间司机可选择拒绝行程，但是这个会涉及到安全
        问题，比如前端脚本注入大量driverId接受行程抢单，这不是目前水平能解决的(或许),故仅注释不删除
         */
            //tripService.acceptTrip(trip.getTripId());

        // 验证订单状态变为已接单
        Order acceptedOrder = orderRepository.findById(order.getOrderId()).orElse(null);
        Assertions.assertEquals(OrderStatus.ACCEPTED, acceptedOrder.getStatus());

        // 验证行程状态
        Trip acceptedTrip = tripRepository.findById(trip.getTripId()).orElse(null);
        Assertions.assertEquals(TripStatus.NOT_STARTED, acceptedTrip.getStatus());

        // -------------------- 步骤3：司机到达起点，行程开始 --------------------
        // 【核心修复】给订单绑定Driver实体（而非User实体），且必须在创建trip前/创建trip后更新trip的order
        // 1. 先给order绑定正确的Driver实体（driver是前面查到的Driver对象）
        order.setDriver(driver); // 原来错写为 order.setDriver(driverUser)
        orderRepository.save(order);

        // 2. 【可选但稳妥】重新查询trip，确保trip关联的是最新的order（避免trip里的order还是旧的）
        trip = tripRepository.findById(trip.getTripId()).orElseThrow();
        // 【关键】传递有效的driverId，且订单已正确关联司机
        tripService.startTrip(trip.getTripId(), driver.getDriverId());

        // 验证行程和订单状态
        Trip ongoingTrip = tripRepository.findById(trip.getTripId()).orElse(null);
        Assertions.assertNotNull(ongoingTrip);
        Assertions.assertEquals(TripStatus.IN_PROGRESS, ongoingTrip.getStatus());

        Order ongoingOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.IN_PROGRESS, ongoingOrder.getStatus());

        // -------------------- 步骤4：到达目的地，结束行程 --------------------
        // 调用completeTrip，传入你方法要求的所有参数
        // 注意：参数根据你的业务逻辑填写，这里用测试用的模拟值
        Double actualDistance = order.getDistance();
        Double actualFare = order.getEstimatedFare();
        Trip completedTrip = tripService.completeTrip(
                trip.getTripId(),          // 行程ID（从已创建的trip获取）
                driver.getDriverId(),      // 司机ID（从已创建的driver获取）
                actualDistance,                      // 实际距离（单位：公里，和订单创建时一致）
                actualFare,                     // 实际费用（=15.5*2.5，对应你的动态定价）
                5,                         // 乘客给司机的评分（1-5分，这里给满分）
                "服务非常好，司机很专业！"  // 乘客反馈（可空，测试用示例）
        );

        // 最终验证
        Assertions.assertNotNull(completedTrip);
        Assertions.assertEquals(TripStatus.COMPLETED, completedTrip.getStatus());
        Assertions.assertEquals(order.getDistance(), completedTrip.getActualDistance());
        Assertions.assertEquals(order.getEstimatedFare(), completedTrip.getActualFare());

        Order completedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
    }

    // ==================== 测试4：动态定价逻辑（单独验证） ====================
    @Test
    void testDynamicPricing() {
        // 这里可以直接测试你的 PriceService 或 OrderService 里的定价逻辑
        // 假设你有一个 calculatePrice 方法
        double basePrice = orderService.calculateEstimatedFare(5.0, OrderType.STANDARD, false); // 5公里 + 标准车
        double peakPrice = orderService.calculateEstimatedFare(5.0, OrderType.STANDARD, true); // 5公里 + 标准车 + 高峰时段

        Assertions.assertTrue(basePrice > 0);
        Assertions.assertTrue(peakPrice > basePrice); // 高峰应该更贵
    }
}