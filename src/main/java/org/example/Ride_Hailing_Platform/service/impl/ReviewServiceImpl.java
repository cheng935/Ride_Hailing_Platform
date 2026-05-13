package org.example.Ride_Hailing_Platform.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.model.order.Order;
import org.example.Ride_Hailing_Platform.model.review.Review;
import org.example.Ride_Hailing_Platform.model.user.User;
import org.example.Ride_Hailing_Platform.repository.OrderRepository;
import org.example.Ride_Hailing_Platform.repository.ReviewRepository;
import org.example.Ride_Hailing_Platform.repository.UserRepository;
import org.example.Ride_Hailing_Platform.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public Review createReview(Long reviewerId, Long reviewedId, Long orderId, Integer rating, String comment) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("评价人不存在"));
        User reviewed = userRepository.findById(reviewedId)
                .orElseThrow(() -> new IllegalArgumentException("被评价人不存在"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }

        Review review = new Review();
        review.setReviewer(reviewer);
        review.setReviewed(reviewed);
        review.setOrder(order);
        review.setRating(rating);
        review.setComment(comment);

        return reviewRepository.save(review);
    }

    @Override
    public List<Review> getReviewsByReviewer(Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return reviewRepository.findByReviewer(reviewer);
    }

    @Override
    public List<Review> getReviewsByReviewed(Long reviewedId) {
        User reviewed = userRepository.findById(reviewedId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return reviewRepository.findByReviewed(reviewed);
    }

    @Override
    public List<Review> getReviewsByOrder(Long orderId) {
        return reviewRepository.findByOrder_OrderId(orderId);
    }
}