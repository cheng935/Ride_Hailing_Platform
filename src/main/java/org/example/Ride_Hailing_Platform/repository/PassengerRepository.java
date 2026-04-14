package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    //通过用户ID查询乘客
    Passenger findByUserId(Long userId);

    //通过手机号查询乘客
    Passenger findByPhone(String phone);

    //
    List<Passenger> findByRideCountGreaterThan(Integer count);
}
