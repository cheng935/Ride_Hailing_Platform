// src/main/java/org/example/Ride_Hailing_Platform/repository/OrderRepository.java
package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.order.OrderStatus;
import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPassengerAndStatusIn(User passenger, List<OrderStatus> status);

    List<Order> findByDriverAndStatusIn(Driver driver, List<OrderStatus> status);

    List<Order> findByStatusIn(List<OrderStatus> status);

    @Query("SELECT o FROM Order o WHERE o.passenger.userId = :userId ORDER BY o.createTime DESC")
    List<Order> findRecentOrdersByPassenger(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.driver.driverId = :userId ORDER BY o.createTime DESC")
    List<Order> findRecentOrdersByDriver(@Param("userId") Long userId);

    boolean existsByPassengerAndStatusIn(User passenger, List<OrderStatus> statusList);

    boolean existsByDriverAndStatusIn(Driver driver, Collection<OrderStatus> status);
}

