package org.example.Ride_Hailing_Platform.repository;

import org.example.Ride_Hailing_Platform.model.review.Review;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewer(User reviewer);
    List<Review> findByReviewed(User reviewed);
    List<Review> findByOrder_OrderId(Long orderId);
}