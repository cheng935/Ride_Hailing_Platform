package org.example.Ride_Hailing_Platform.repository;


import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface DriverRepository extends JpaRepository<Driver, Long> {
    //查询所有在线可接单司机
    List<Driver> findByIsOnlineTrue();

    //查询第一个在线可接单司机
    Optional<Driver> findFirstByIsOnlineTrue();

    @Query("SELECT d FROM Driver d WHERE d.isOnline = true AND d.vehicleType = :vehicleType")
    List<Driver> findIsOnlineByVehicleType(@Param("vehicleType") String vehicleType);

    //通过手机号查询司机
    Optional<Driver> findByPhone(String phone);
}
