package org.example.ridehailing.repository;

import org.example.ridehailing.model.review.Review;
import org.example.ridehailing.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewer(User reviewer);
    List<Review> findByReviewed(User reviewed);
    List<Review> findByOrder_OrderId(Long orderId);
}