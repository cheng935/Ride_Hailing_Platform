package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
