// src/main/java/org/example/Ride_Hailing_Platform/repository/TripRepository.java
package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.trip.Trip;
import org.example.Ride_Hailing_Platform.model.trip.TripStatus;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t WHERE t.order.orderId = :orderId")
    Optional<Trip> findByOrderId(@Param("orderId") Long orderId);

    List<Trip> findByOrderPassengerAndStatus(Passenger passenger, TripStatus status);
    List<Trip> findByOrderDriverAndStatus(Driver driver, TripStatus status);

    //查询多个状态的乘客行程
    List<Trip> findByOrderPassengerAndStatusIn(
            Passenger passenger,
            List<TripStatus> statusList
    );

    //查询多个状态的司机行程
    List<Trip> findByOrderDriverAndStatusIn(
            Driver driver,
            List<TripStatus> statusList
    );

    @Query("SELECT t FROM Trip t WHERE t.order.passenger.userId = :userId ORDER BY t.createTime DESC")
    List<Trip> findRecentTripsByPassenger(@Param("userId") Long userId);

    @Query("SELECT t FROM Trip t WHERE t.order.driver.userId = :userId ORDER BY t.createTime DESC")
    List<Trip> findRecentTripsByDriver(@Param("userId") Long userId);

    long countByOrderDriverAndStatus(User driver, TripStatus status);
}