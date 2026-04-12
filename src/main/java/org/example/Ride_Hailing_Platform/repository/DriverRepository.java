package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.user.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByIsAvailableTrue();

    @Query("SELECT d FROM Driver d WHERE d.isAvailable = true AND d.vehicleType = :vehicleType")
    List<Driver> findAvailableByVehicleType(@Param("vehicleType") String vehicleType);
}
