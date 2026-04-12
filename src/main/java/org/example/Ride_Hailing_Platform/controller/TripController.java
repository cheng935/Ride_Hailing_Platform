// src/main/java/org/example/Ride_Hailing_Platform/controller/TripController.java
package org.example.Ride_Hailing_Platform.controller;

import org.example.Ride_Hailing_Platform.model.trip.*;
import org.example.Ride_Hailing_Platform.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping("/from-order/{orderId}")
    public ResponseEntity<Trip> createTripFromOrder(@PathVariable Long orderId) {
        try {
            Trip trip = tripService.createTripFromOrder(orderId);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{tripId}/start")
    public ResponseEntity<Trip> startTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {
        try {
            Trip trip = tripService.startTrip(tripId, driverId);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{tripId}/update-location")
    public ResponseEntity<Trip> updateDriverLocation(
            @PathVariable Long tripId,
            @RequestBody Location location) {
        try {
            Trip trip = tripService.updateDriverLocation(tripId, location);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{tripId}/complete")
    public ResponseEntity<Trip> completeTrip(
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
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long tripId) {
        return tripService.findTripById(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Trip>> getActiveTrips(
            @RequestParam Long userId,
            @RequestParam boolean isPassenger) {
        List<Trip> trips = tripService.getActiveTrips(userId, isPassenger);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/stats/{driverId}")
    public ResponseEntity<TripService.DriverTripStats> getDriverStats(@PathVariable Long driverId) {
        try {
            TripService.DriverTripStats stats = tripService.getDriverTripStats(driverId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
