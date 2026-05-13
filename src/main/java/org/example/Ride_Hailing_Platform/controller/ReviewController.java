package org.example.Ride_Hailing_Platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.Ride_Hailing_Platform.common.ApiResponse;
import org.example.Ride_Hailing_Platform.model.review.Review;
import org.example.Ride_Hailing_Platform.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "评价管理", description = "用户评价相关接口")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "创建评价", description = "对订单中的司机或乘客进行评价")
    public ApiResponse<Review> createReview(
            @RequestParam Long reviewerId,
            @RequestParam Long reviewedId,
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {
        Review review = reviewService.createReview(reviewerId, reviewedId, orderId, rating, comment);
        return ApiResponse.success("评价成功", review);
    }

    @GetMapping("/reviewer/{reviewerId}")
    @Operation(summary = "查询发出的评价", description = "查询某用户发出的所有评价")
    public ApiResponse<List<Review>> getReviewsByReviewer(@PathVariable Long reviewerId) {
        List<Review> reviews = reviewService.getReviewsByReviewer(reviewerId);
        return ApiResponse.success(reviews);
    }

    @GetMapping("/reviewed/{reviewedId}")
    @Operation(summary = "查询收到的评价", description = "查询某用户收到的所有评价")
    public ApiResponse<List<Review>> getReviewsByReviewed(@PathVariable Long reviewedId) {
        List<Review> reviews = reviewService.getReviewsByReviewed(reviewedId);
        return ApiResponse.success(reviews);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单评价", description = "查询某订单的所有评价")
    public ApiResponse<List<Review>> getReviewsByOrder(@PathVariable Long orderId) {
        List<Review> reviews = reviewService.getReviewsByOrder(orderId);
        return ApiResponse.success(reviews);
    }
}