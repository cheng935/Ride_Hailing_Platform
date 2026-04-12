package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.service.OrderService;
import org.example.Ride_Hailing_Platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/{passengerId}/orders")
    public ResponseEntity<Order> requestRide(
            @PathVariable Long passengerId,
            @RequestParam String startAddress,
            @RequestParam String endAddress) {

        User user = userService.findUserById(passengerId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Passenger passenger = new Passenger();
        passenger.setUser(user);

        Order order = orderService.requestRide(passenger, startAddress, endAddress);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{passengerId}/orders")
    public ResponseEntity<List<Order>> getPassengerOrders(@PathVariable Long passengerId) {
        // 这里需要完善，根据实际业务逻辑
        return ResponseEntity.ok(List.of());
    }
}
