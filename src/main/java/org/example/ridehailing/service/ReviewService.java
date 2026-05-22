package org.example.ridehailing.service;

import org.example.ridehailing.model.review.Review;

import java.util.List;

public interface ReviewService {
    Review createReview(Long reviewerId, Long reviewedId, Long orderId, Integer rating, String comment);
    List<Review> getReviewsByReviewer(Long reviewerId);
    List<Review> getReviewsByReviewed(Long reviewedId);
    List<Review> getReviewsByOrder(Long orderId);
}