package org.example.ridehailing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.common.ApiResponse;
import org.example.ridehailing.dto.PricingRequest;
import org.example.ridehailing.dto.PricingResponse;
import org.example.ridehailing.service.pricing.DynamicPricingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@Tag(name = "动态计价", description = "网约车动态价格计算接口")
public class PricingController {

    private final DynamicPricingService dynamicPricingService;

    @PostMapping("/estimate")
    @Operation(summary = "预估价格", description = "根据起终点计算动态价格，返回基础价+各项溢价明细+最终总价")
    public ApiResponse<PricingResponse> estimatePrice(@RequestBody PricingRequest request) {
        if (request.getPickupLat() == null || request.getPickupLng() == null) {
            return ApiResponse.error("出发地坐标不能为空");
        }
        if (request.getDestLat() == null || request.getDestLng() == null) {
            return ApiResponse.error("目的地坐标不能为空");
        }

        PricingResponse response = dynamicPricingService.calculatePrice(request);
        return ApiResponse.success(response);
    }
}
