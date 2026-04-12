package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final OrderServiceImpl orderServiceImpl;

    @GetMapping("/available-orders")
    public ResponseEntity<List<Order>> getAvailableOrders() {
        List<Order> pendingOrders = orderServiceImpl.getPendingOrders();
        return ResponseEntity.ok(pendingOrders);
    }

    @PutMapping("/{driverId}/orders/{orderId}/accept")
    public ResponseEntity<Order> acceptOrder(
            @PathVariable Long driverId,
            @PathVariable Long orderId) {
        try {
            Order order = orderServiceImpl.acceptOrder(driverId, orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
