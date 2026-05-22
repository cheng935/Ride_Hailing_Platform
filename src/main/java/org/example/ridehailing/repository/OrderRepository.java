// src/main/java/org/example/Ride_Hailing_Platform/repository/OrderRepository.java
package org.example.ridehailing.repository;

import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.order.OrderStatus;
import org.example.ridehailing.model.user.Driver;
import org.example.ridehailing.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPassengerAndStatusIn(User passenger, List<OrderStatus> status);

    List<Order> findByDriverAndStatusIn(Driver driver, List<OrderStatus> status);

    List<Order> findByStatusIn(List<OrderStatus> status);

    @Query("SELECT o FROM Order o WHERE o.passenger.userId = :userId ORDER BY o.createTime DESC")
    List<Order> findRecentOrdersByPassenger(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.driver.userId = :userId ORDER BY o.createTime DESC")
    List<Order> findRecentOrdersByDriver(@Param("userId") Long userId);

    boolean existsByPassengerAndStatusIn(User passenger, List<OrderStatus> statusList);

    boolean existsByDriverAndStatusIn(Driver driver, Collection<OrderStatus> status);
}

