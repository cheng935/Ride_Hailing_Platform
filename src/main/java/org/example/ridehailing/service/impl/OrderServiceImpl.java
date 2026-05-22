package org.example.ridehailing.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.dto.OrderDetailDTO;
import org.example.ridehailing.dto.PaymentRequest;
import org.example.ridehailing.dto.PaymentResponse;
import org.example.ridehailing.dto.PricingRequest;
import org.example.ridehailing.dto.PricingResponse;
import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.order.OrderStatus;
import org.example.ridehailing.model.order.OrderType;
import org.example.ridehailing.model.payment.Payment;
import org.example.ridehailing.model.payment.PaymentStatus;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.Passenger;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.repository.OrderRepository;
import org.example.ridehailing.repository.UserRepository;
import org.example.ridehailing.service.OrderService;
import org.example.ridehailing.service.PaymentService;
import org.example.ridehailing.service.cache.PendingOrderCacheService;
import org.example.ridehailing.service.pricing.DynamicPricingService;
import org.example.ridehailing.service.pubsub.RedisPubSubService;
import org.example.ridehailing.util.GeoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final PendingOrderCacheService pendingOrderCacheService;
    private final RedisPubSubService redisPubSubService;
    private final DynamicPricingService dynamicPricingService;

    @Override
    public Order createOrder(Long passengerId, String pickupName, Double pickupLat, Double pickupLng,
                             String destName, Double destLat, Double destLng) {
        User passenger = userRepository.findById(passengerId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.ACCEPTED,
                OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS);
        List<Order> activeOrders = orderRepository.findByPassengerAndStatusIn(passenger, activeStatuses);
        if (!activeOrders.isEmpty()) {
            throw BusinessException.badRequest("您有未完成的订单");
        }

        PricingRequest pricingRequest = new PricingRequest();
        pricingRequest.setPickupLat(pickupLat);
        pricingRequest.setPickupLng(pickupLng);
        pricingRequest.setDestLat(destLat);
        pricingRequest.setDestLng(destLng);
        pricingRequest.setPickupName(pickupName);
        pricingRequest.setDestName(destName);

        PricingResponse pricingResponse = dynamicPricingService.calculatePrice(pricingRequest);
        double fare = pricingResponse.getFinalFare();

        double distance = GeoUtil.haversineDistance(pickupLat, pickupLng, destLat, destLng);
        if (pricingResponse.getDistanceKm() > 0) {
            distance = pricingResponse.getDistanceKm();
        }

        Order order = new Order();
        order.setPassenger(passenger);
        order.setPickupLocation(pickupName);
        order.setPickupLat(pickupLat);
        order.setPickupLng(pickupLng);
        order.setDestination(destName);
        order.setDestLat(destLat);
        order.setDestLng(destLng);
        order.setDistance(Math.round(distance * 100.0) / 100.0);
        order.setEstimatedFare(fare);
        order.setStatus(OrderStatus.PENDING);
        order.setType(OrderType.STANDARD);
        order.setCreateTime(LocalDateTime.now());

        order = orderRepository.save(order);

        pendingOrderCacheService.addPendingOrder(order);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", order.getStatus().name());
        extra.put("pickupName", order.getPickupLocation());
        extra.put("destName", order.getDestination());
        extra.put("estimatedFare", order.getEstimatedFare());
        redisPubSubService.publishOrderEvent("CREATED", order.getOrderId(),
                passenger.getUserId(), null, extra);

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getPendingOrders() {
        List<Map<String, Object>> cached = pendingOrderCacheService.getPendingOrders();
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().map(this::fromCachedMap).collect(Collectors.toList());
        }
        List<Map<String, Object>> refreshed = pendingOrderCacheService.refreshFromDatabase();
        return refreshed.stream().map(this::fromCachedMap).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetail(Long orderId) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        return toDTO(o);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getMyOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        List<Order> orders;
        if (user instanceof Driver) {
            orders = orderRepository.findRecentOrdersByDriver(userId);
        } else {
            orders = orderRepository.findRecentOrdersByPassenger(userId);
        }

        return orders.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Order acceptOrder(Long orderId, Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (!(user instanceof Driver driver)) {
            throw BusinessException.badRequest("不是司机账号");
        }
        if (!driver.getOnline()) {
            throw BusinessException.badRequest("请先上线");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw BusinessException.badRequest("订单已被接走");
        }

        order.setDriver(driver);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptTime(LocalDateTime.now());
        orderRepository.save(order);

        pendingOrderCacheService.removePendingOrder(orderId);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", OrderStatus.ACCEPTED.name());
        extra.put("driverName", driver.getName());
        extra.put("driverPhone", driver.getPhone());
        extra.put("vehiclePlate", driver.getVehiclePlate());
        redisPubSubService.publishOrderEvent("ACCEPTED", orderId,
                order.getPassenger().getUserId(), driver.getUserId(), extra);

        return order;
    }

    @Override
    public Order arrivePickup(Long orderId, Long driverId) {
        Driver driver = resolveDriver(driverId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw BusinessException.badRequest("订单状态不正确");
        }

        order.setStatus(OrderStatus.PICKING_UP);
        orderRepository.save(order);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", OrderStatus.PICKING_UP.name());
        redisPubSubService.publishOrderEvent("ARRIVED", orderId,
                order.getPassenger().getUserId(), driver.getUserId(), extra);

        return order;
    }

    @Override
    public Order startTrip(Long orderId, Long driverId) {
        Driver driver = resolveDriver(driverId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.PICKING_UP) {
            throw BusinessException.badRequest("订单状态不正确");
        }

        order.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(order);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", OrderStatus.IN_PROGRESS.name());
        redisPubSubService.publishOrderEvent("STARTED", orderId,
                order.getPassenger().getUserId(), driver.getUserId(), extra);

        return order;
    }

    @Override
    public OrderDetailDTO completeTrip(Long orderId, Long driverId) {
        Driver driver = resolveDriver(driverId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("订单状态不正确");
        }

        PricingRequest pricingRequest = new PricingRequest();
        pricingRequest.setPickupLat(order.getPickupLat());
        pricingRequest.setPickupLng(order.getPickupLng());
        pricingRequest.setDestLat(order.getDestLat());
        pricingRequest.setDestLng(order.getDestLng());
        pricingRequest.setPickupName(order.getPickupLocation());
        pricingRequest.setDestName(order.getDestination());

        PricingResponse pricingResponse = dynamicPricingService.calculatePrice(pricingRequest);
        double actualFare = pricingResponse.getFinalFare();

        order.setActualFare(actualFare);
        order.setBaseFare(pricingResponse.getBaseFare());
        order.setDistanceFare(pricingResponse.getDistanceFare());
        order.setDurationFare(pricingResponse.getDurationFare());

        double subtotal = pricingResponse.getSubtotal();
        if (pricingResponse.getSurcharges() != null && !pricingResponse.getSurcharges().isEmpty()) {
            List<Order.SurchargeItem> items = pricingResponse.getSurcharges().stream()
                    .map(s -> new Order.SurchargeItem(s.getType(), s.getReason(),
                            Math.round((subtotal * (s.getMultiplier() - 1)) * 100.0) / 100.0))
                    .toList();
            order.setSurcharges(items);
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);

        pendingOrderCacheService.evictOrderDetail(orderId);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", OrderStatus.COMPLETED.name());
        extra.put("actualFare", actualFare);
        redisPubSubService.publishOrderEvent("COMPLETED", orderId,
                order.getPassenger().getUserId(), driver.getUserId(), extra);

        return toDTO(order);
    }

    @Override
    public OrderDetailDTO payOrder(Long orderId, Long passengerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getPassenger() == null || !order.getPassenger().getUserId().equals(passengerId)) {
            throw BusinessException.forbidden("无权支付此订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.badRequest("订单未完成");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw BusinessException.badRequest("已支付");
        }

        PaymentRequest req = new PaymentRequest();
        req.setOrderId(orderId);
        req.setPaymentMethod("微信支付");

        paymentService.initiatePayment(req);

        return toDTO(order);
    }

    @Override
    public OrderDetailDTO confirmPay(Long orderId, Long passengerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getPassenger() == null || !order.getPassenger().getUserId().equals(passengerId)) {
            throw BusinessException.forbidden("无权操作此订单");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw BusinessException.badRequest("已支付");
        }

        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            throw BusinessException.badRequest("未找到支付记录，请先发起支付");
        }

        paymentService.confirmPayment(payment.getPaymentId());

        orderRepository.findById(orderId);
        return getOrderDetail(orderId);
    }

    @Override
    public Order cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        boolean isPassenger = order.getPassenger() != null && order.getPassenger().getUserId().equals(userId);
        boolean isDriver = order.getDriver() != null && order.getDriver().getUserId().equals(userId);

        if (!isPassenger && !isDriver) {
            throw BusinessException.forbidden("无权取消此订单");
        }
        if (!order.canBeCancelled()) {
            throw BusinessException.badRequest("当前订单状态不允许取消");
        }

        boolean wasPending = order.getStatus() == OrderStatus.PENDING;

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason != null ? reason : (isDriver ? "司机主动取消" : "乘客主动取消"));
        orderRepository.save(order);

        if (wasPending) {
            pendingOrderCacheService.removePendingOrder(orderId);
        }
        pendingOrderCacheService.evictOrderDetail(orderId);

        Map<String, Object> extra = new HashMap<>();
        extra.put("status", OrderStatus.CANCELLED.name());
        extra.put("reason", reason);
        redisPubSubService.publishOrderEvent("CANCELLED", orderId,
                order.getPassenger() != null ? order.getPassenger().getUserId() : null,
                order.getDriver() != null ? order.getDriver().getUserId() : null,
                extra);

        return order;
    }

    @Override
    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getActiveOrders(Long userId, boolean isPassenger) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.ACCEPTED,
                OrderStatus.PICKING_UP, OrderStatus.IN_PROGRESS);

        if (isPassenger) {
            Passenger passenger = (Passenger) user;
            return orderRepository.findByPassengerAndStatusIn(passenger, activeStatuses);
        } else {
            Driver driver = (Driver) user;
            return orderRepository.findByDriverAndStatusIn(driver, activeStatuses);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getHistoryOrders(Long userId, boolean isPassenger) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        List<OrderStatus> historyStatuses = List.of(
                OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REJECTED);

        if (isPassenger) {
            Passenger passenger = (Passenger) user;
            return orderRepository.findByPassengerAndStatusIn(passenger, historyStatuses);
        } else {
            Driver driver = (Driver) user;
            return orderRepository.findByDriverAndStatusIn(driver, historyStatuses);
        }
    }

    @Override
    public PricingResponse estimatePrice(Double pickupLat, Double pickupLng,
                                         Double destLat, Double destLng,
                                         String pickupName, String destName) {
        PricingRequest request = new PricingRequest();
        request.setPickupLat(pickupLat);
        request.setPickupLng(pickupLng);
        request.setDestLat(destLat);
        request.setDestLng(destLng);
        request.setPickupName(pickupName);
        request.setDestName(destName);
        return dynamicPricingService.calculatePrice(request);
    }

    private OrderDetailDTO toDTO(Order o) {
        return OrderDetailDTO.builder()
                .orderId(o.getOrderId())
                .status(o.getStatus().name())
                .passengerName(o.getPassenger() != null ? o.getPassenger().getName() : null)
                .passengerPhone(o.getPassenger() != null ? o.getPassenger().getPhone() : null)
                .driverName(o.getDriver() != null ? o.getDriver().getName() : null)
                .driverId(o.getDriver() != null ? o.getDriver().getUserId() : null)
                .driverPhone(o.getDriver() != null ? o.getDriver().getPhone() : null)
                .vehiclePlate(o.getDriver() != null ? o.getDriver().getVehiclePlate() : null)
                .pickupName(o.getPickupLocation())
                .pickupLat(o.getPickupLat())
                .pickupLng(o.getPickupLng())
                .destName(o.getDestination())
                .destLat(o.getDestLat())
                .destLng(o.getDestLng())
                .distance(o.getDistance())
                .estimatedFare(o.getEstimatedFare())
                .actualFare(o.getActualFare())
                .paymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID")
                .createTime(o.getCreateTime())
                .baseFare(o.getBaseFare())
                .distanceFare(o.getDistanceFare())
                .durationFare(o.getDurationFare())
                .surcharges(convertSurcharges(o.getSurcharges()))
                .build();
    }

    @SuppressWarnings("unchecked")
    private OrderDetailDTO fromCachedMap(Map<String, Object> m) {
        return OrderDetailDTO.builder()
                .orderId(m.get("orderId") != null ? ((Number) m.get("orderId")).longValue() : null)
                .status((String) m.get("status"))
                .passengerName((String) m.get("passengerName"))
                .driverName((String) m.get("driverName"))
                .driverId(m.get("driverId") != null ? ((Number) m.get("driverId")).longValue() : null)
                .pickupName((String) m.get("pickupName"))
                .destName((String) m.get("destName"))
                .distance(m.get("distance") != null ? ((Number) m.get("distance")).doubleValue() : null)
                .estimatedFare(m.get("estimatedFare") != null ? ((Number) m.get("estimatedFare")).doubleValue() : null)
                .paymentStatus((String) m.get("paymentStatus"))
                .build();
    }

    private Driver resolveDriver(Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (!(user instanceof Driver driver)) {
            throw BusinessException.badRequest("不是司机账号");
        }
        return driver;
    }

    private List<OrderDetailDTO.SurchargeItem> convertSurcharges(List<Order.SurchargeItem> items) {
        if (items == null || items.isEmpty()) return null;
        return items.stream()
                .map(s -> OrderDetailDTO.SurchargeItem.builder()
                        .type(s.getType())
                        .reason(s.getReason())
                        .amount(s.getAmount())
                        .build())
                .toList();
    }
}
