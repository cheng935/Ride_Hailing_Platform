// src/main/java/org/example/Ride_Hailing_Platform/controller/OrderController.java
package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderType;
import org.example.Ride_Hailing_Platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(
            @RequestParam Long passengerId,
            @RequestParam String pickupLocation,
            @RequestParam String destination,
            @RequestParam OrderType type) {
        try {
            Order order = orderService.createOrder(passengerId, pickupLocation, destination, type);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<Order> acceptOrder(
            @PathVariable Long orderId,
            @RequestParam Long driverId) {
        try {
            Order order = orderService.acceptOrder(orderId, driverId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId) {
        try {
            Order order = orderService.cancelOrder(orderId, userId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        return orderService.findOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Order>> getActiveOrders(
            @RequestParam Long userId,
            @RequestParam boolean isPassenger) {
        List<Order> orders = orderService.getActiveOrders(userId, isPassenger);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getHistoryOrders(
            @RequestParam Long userId,
            @RequestParam boolean isPassenger) {
        List<Order> orders = orderService.getHistoryOrders(userId, isPassenger);
        return ResponseEntity.ok(orders);
    }
}
