package org.example.Ride_Hailing_Platform.service;

import org.example.Ride_Hailing_Platform.model.review.Review;

import java.util.List;

public interface ReviewService {
    Review createReview(Long reviewerId, Long reviewedId, Long orderId, Integer rating, String comment);
    List<Review> getReviewsByReviewer(Long reviewerId);
    List<Review> getReviewsByReviewed(Long reviewedId);
    List<Review> getReviewsByOrder(Long orderId);
}