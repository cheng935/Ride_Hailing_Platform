package org.example.ridehailing.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long reviewerId;
    private Long reviewedId;
    private Long orderId;
    private Integer rating;
    private String comment;
}
