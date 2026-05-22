package org.example.ridehailing.service;

import org.example.ridehailing.dto.OrderDetailDTO;
import org.example.ridehailing.dto.PricingResponse;
import org.example.ridehailing.model.order.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Long passengerId, String pickupName, Double pickupLat, Double pickupLng,
                      String destName, Double destLat, Double destLng);

    List<OrderDetailDTO> getPendingOrders();

    OrderDetailDTO getOrderDetail(Long orderId);

    List<OrderDetailDTO> getMyOrders(Long userId);

    Order acceptOrder(Long orderId, Long driverId);

    Order arrivePickup(Long orderId, Long driverId);

    Order startTrip(Long orderId, Long driverId);

    OrderDetailDTO completeTrip(Long orderId, Long driverId);

    OrderDetailDTO payOrder(Long orderId, Long passengerId);

    OrderDetailDTO confirmPay(Long orderId, Long passengerId);

    Order cancelOrder(Long orderId, Long userId, String reason);

    Optional<Order> findOrderById(Long orderId);

    List<Order> getActiveOrders(Long userId, boolean isPassenger);

    List<Order> getHistoryOrders(Long userId, boolean isPassenger);

    PricingResponse estimatePrice(Double pickupLat, Double pickupLng,
                                  Double destLat, Double destLng,
                                  String pickupName, String destName);
}
