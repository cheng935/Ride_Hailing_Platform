package org.example.ridehailing.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.exception.BusinessException;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.review.Review;
import org.example.ridehailing.model.user.User;
import org.example.ridehailing.repository.OrderRepository;
import org.example.ridehailing.repository.ReviewRepository;
import org.example.ridehailing.repository.UserRepository;
import org.example.ridehailing.service.ReviewService;
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
                .orElseThrow(() -> BusinessException.notFound("评价人不存在"));
        User reviewed = userRepository.findById(reviewedId)
                .orElseThrow(() -> BusinessException.notFound("被评价人不存在"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (rating < 1 || rating > 5) {
            throw BusinessException.badRequest("评分必须在1-5之间");
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
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return reviewRepository.findByReviewer(reviewer);
    }

    @Override
    public List<Review> getReviewsByReviewed(Long reviewedId) {
        User reviewed = userRepository.findById(reviewedId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return reviewRepository.findByReviewed(reviewed);
    }

    @Override
    public List<Review> getReviewsByOrder(Long orderId) {
        return reviewRepository.findByOrder_OrderId(orderId);
    }
}
