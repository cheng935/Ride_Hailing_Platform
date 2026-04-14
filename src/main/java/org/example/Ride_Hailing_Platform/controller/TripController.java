package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.trip.*;
import org.example.Ride_Hailing_Platform.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping("/from-order/{orderId}")
    public ResponseEntity<?> createTripFromOrder(@PathVariable Long orderId) {
        try {
            Trip trip = tripService.createTripFromOrder(orderId);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 优化：返回错误信息，而非null
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{tripId}/start")
    public ResponseEntity<?> startTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {
        try {
            Trip trip = tripService.startTrip(tripId, driverId);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{tripId}/update-location")
    public ResponseEntity<?> updateDriverLocation(
            @PathVariable Long tripId,
            @RequestBody Location location) {
        try {
            Trip trip = tripService.updateDriverLocation(tripId, location);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{tripId}/complete")
    public ResponseEntity<?> completeTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId,
            @RequestParam Double actualDistance,
            @RequestParam Double actualFare,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String feedback) {
        try {
            Trip trip = tripService.completeTrip(tripId, driverId, actualDistance, actualFare, rating, feedback);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long tripId) {
        return tripService.findTripById(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveTrips(
            @RequestParam Long userId,
            @RequestParam boolean isPassenger) {
        try {
            List<Trip> trips = tripService.getActiveTrips(userId, isPassenger);
            return ResponseEntity.ok(trips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/stats/{driverId}")
    public ResponseEntity<?> getDriverStats(@PathVariable Long driverId) {
        try {
            TripService.DriverTripStats stats = tripService.getDriverTripStats(driverId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 全局异常处理,抽离到单独的ExceptionHandler类更好
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器内部错误：" + e.getMessage());
    }
}