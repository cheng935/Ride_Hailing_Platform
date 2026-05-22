package org.example.ridehailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.dto.ReviewRequest;
import org.example.ridehailing.model.review.Review;
import org.example.ridehailing.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "评价管理", description = "用户评价相关接口")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "创建评价", description = "对订单中的司机或乘客进行评价")
    public ApiResponse<Map<String, Object>> createReview(@RequestBody ReviewRequest req) {
        Review review = reviewService.createReview(
                req.getReviewerId(), req.getReviewedId(),
                req.getOrderId(), req.getRating(), req.getComment());
        return ApiResponse.success("评价成功", toMap(review));
    }

    @GetMapping("/reviewer/{reviewerId}")
    @Operation(summary = "查询发出的评价", description = "查询某用户发出的所有评价")
    public ApiResponse<List<Map<String, Object>>> getReviewsByReviewer(@PathVariable Long reviewerId) {
        List<Review> reviews = reviewService.getReviewsByReviewer(reviewerId);
        return ApiResponse.success(reviews.stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/reviewed/{reviewedId}")
    @Operation(summary = "查询收到的评价", description = "查询某用户收到的所有评价")
    public ApiResponse<List<Map<String, Object>>> getReviewsByReviewed(@PathVariable Long reviewedId) {
        List<Review> reviews = reviewService.getReviewsByReviewed(reviewedId);
        return ApiResponse.success(reviews.stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单评价", description = "查询某订单的所有评价")
    public ApiResponse<List<Map<String, Object>>> getReviewsByOrder(@PathVariable Long orderId) {
        List<Review> reviews = reviewService.getReviewsByOrder(orderId);
        return ApiResponse.success(reviews.stream().map(this::toMap).collect(Collectors.toList()));
    }

    private Map<String, Object> toMap(Review r) {
        Map<String, Object> m = new HashMap<>();
        m.put("reviewId", r.getReviewId());
        m.put("reviewerId", r.getReviewer().getUserId());
        m.put("reviewerName", r.getReviewer().getName());
        m.put("reviewedId", r.getReviewed().getUserId());
        m.put("reviewedName", r.getReviewed().getName());
        m.put("orderId", r.getOrder().getOrderId());
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : null);
        return m;
    }
}