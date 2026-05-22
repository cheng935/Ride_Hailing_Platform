package org.example.ridehailing.repository;

import org.example.ridehailing.model.user.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByOnlineTrue();

    Optional<Driver> findByUserId(Long userId);

    Optional<Driver> findFirstByOnlineTrue();

    @Query("SELECT d FROM Driver d WHERE d.online = true AND d.vehicleType = :vehicleType")
    List<Driver> findOnlineByVehicleType(@Param("vehicleType") String vehicleType);

    Optional<Driver> findByPhone(String phone);
}
