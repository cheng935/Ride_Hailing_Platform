package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.user.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}
