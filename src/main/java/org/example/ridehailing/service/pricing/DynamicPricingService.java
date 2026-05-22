package org.example.ridehailing.service.pricing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridehailing.dto.PricingRequest;
import org.example.ridehailing.dto.PricingResponse;
import org.example.ridehailing.service.amap.AmapApiService;
import org.example.ridehailing.util.GeoUtil;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPricingService {

    private static final double BASE_FARE = 8.0;
    private static final double PER_KM_RATE = 2.0;
    private static final double PER_MIN_RATE = 0.5;
    private static final double MAX_TOTAL_MULTIPLIER = 2.5;

    private static final double PEAK_MULTIPLIER = 1.3;
    private static final double NIGHT_MULTIPLIER = 1.4;
    private static final double WEEKEND_MULTIPLIER = 1.1;
    private static final double HOLIDAY_MULTIPLIER = 1.1;

    private static final double CONGESTION_LEVEL2_MULTIPLIER = 1.2;
    private static final double CONGESTION_LEVEL3_MULTIPLIER = 1.5;
    private static final double CONGESTION_LEVEL4_MULTIPLIER = 2.0;

    private static final double WEATHER_MULTIPLIER = 1.3;

    private static final Set<String> BAD_WEATHER_KEYWORDS = Set.of(
            "雨", "雪", "雾", "霾", "暴雨", "大雨", "中雨", "小雨", "雷阵雨",
            "阵雪", "中雪", "大雪", "暴雪", "大雾", "浓雾", "沙尘", "冰雹",
            "台风", "大风", "暴风", "冻雨"
    );

    private static final Set<LocalDate> HOLIDAYS_2025 = Set.of(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 28), LocalDate.of(2025, 1, 29),
            LocalDate.of(2025, 1, 30), LocalDate.of(2025, 1, 31),
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 2), LocalDate.of(2025, 2, 3),
            LocalDate.of(2025, 2, 4),
            LocalDate.of(2025, 4, 4), LocalDate.of(2025, 4, 5), LocalDate.of(2025, 4, 6),
            LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2), LocalDate.of(2025, 5, 3),
            LocalDate.of(2025, 5, 4), LocalDate.of(2025, 5, 5),
            LocalDate.of(2025, 5, 31), LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2),
            LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 2), LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 4), LocalDate.of(2025, 10, 5), LocalDate.of(2025, 10, 6),
            LocalDate.of(2025, 10, 7), LocalDate.of(2025, 10, 8)
    );

    private final AmapApiService amapApiService;

    public PricingResponse calculatePrice(PricingRequest request) {
        PricingResponse response = new PricingResponse();
        List<PricingResponse.SurchargeDetail> surcharges = new ArrayList<>();

        double distanceKm = GeoUtil.haversineDistance(
                request.getPickupLat(), request.getPickupLng(),
                request.getDestLat(), request.getDestLng());

        AmapApiService.RouteResult routeResult = amapApiService.getRouteInfo(
                request.getPickupLng(), request.getPickupLat(),
                request.getDestLng(), request.getDestLat());

        if (routeResult.isSuccess() && routeResult.getDistanceKm() > 0) {
            distanceKm = routeResult.getDistanceKm();
        }

        double durationMin = distanceKm / 30.0 * 60.0;
        if (routeResult.isSuccess() && routeResult.getDurationMinutes() > 0) {
            durationMin = routeResult.getDurationMinutes();
        }

        double distanceFare = Math.round(distanceKm * PER_KM_RATE * 100.0) / 100.0;
        double durationFare = Math.round(durationMin * PER_MIN_RATE * 100.0) / 100.0;
        double subtotal = Math.round((BASE_FARE + distanceFare + durationFare) * 100.0) / 100.0;

        response.setBaseFare(BASE_FARE);
        response.setDistanceKm(Math.round(distanceKm * 100.0) / 100.0);
        response.setDistanceFare(distanceFare);
        response.setDurationMinutes(Math.round(durationMin * 10.0) / 10.0);
        response.setDurationFare(durationFare);
        response.setSubtotal(subtotal);

        LocalDateTime now = LocalDateTime.now();
        applyTimeSurcharge(now, surcharges);
        applyCongestionSurcharge(routeResult, surcharges);
        applyWeatherSurcharge(request, surcharges);

        double totalMultiplier = 1.0;
        for (PricingResponse.SurchargeDetail s : surcharges) {
            totalMultiplier *= s.getMultiplier();
        }
        totalMultiplier = Math.round(totalMultiplier * 100.0) / 100.0;

        double totalFare = Math.round(subtotal * totalMultiplier * 100.0) / 100.0;
        double cappedFare = Math.round(subtotal * MAX_TOTAL_MULTIPLIER * 100.0) / 100.0;
        double finalFare = Math.min(totalFare, cappedFare);
        finalFare = Math.round(finalFare * 100.0) / 100.0;

        response.setSurcharges(surcharges);
        response.setTotalMultiplier(totalMultiplier);
        response.setTotalFare(totalFare);
        response.setCappedFare(cappedFare);
        response.setFinalFare(finalFare);

        return response;
    }

    private void applyTimeSurcharge(LocalDateTime now, List<PricingResponse.SurchargeDetail> surcharges) {
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean isHoliday = HOLIDAYS_2025.contains(date);

        if (isHoliday) {
            addSurcharge(surcharges, "time", "节假日加价", HOLIDAY_MULTIPLIER);
        } else if (isWeekend) {
            addSurcharge(surcharges, "time", "周末加价", WEEKEND_MULTIPLIER);
        } else if (hour >= 7 && hour < 9) {
            addSurcharge(surcharges, "time", "早高峰加价(7:00-9:00)", PEAK_MULTIPLIER);
        } else if (hour >= 17 && hour < 20) {
            addSurcharge(surcharges, "time", "晚高峰加价(17:00-20:00)", PEAK_MULTIPLIER);
        }

        if (hour >= 23 || hour < 6) {
            addSurcharge(surcharges, "time", "夜间加价(23:00-6:00)", NIGHT_MULTIPLIER);
        }
    }

    private void applyCongestionSurcharge(AmapApiService.RouteResult routeResult,
                                          List<PricingResponse.SurchargeDetail> surcharges) {
        if (!routeResult.isSuccess()) {
            return;
        }

        int level = routeResult.getAvgCongestionLevel();
        switch (level) {
            case 2 -> addSurcharge(surcharges, "traffic", "缓行加价(拥堵等级2)", CONGESTION_LEVEL2_MULTIPLIER);
            case 3 -> addSurcharge(surcharges, "traffic", "拥堵加价(拥堵等级3)", CONGESTION_LEVEL3_MULTIPLIER);
            case 4 -> addSurcharge(surcharges, "traffic", "严重拥堵加价(拥堵等级4)", CONGESTION_LEVEL4_MULTIPLIER);
        }
    }

    private void applyWeatherSurcharge(PricingRequest request,
                                       List<PricingResponse.SurchargeDetail> surcharges) {
        String adcode = inferAdcode(request.getPickupLat(), request.getPickupLng());
        AmapApiService.WeatherResult weather = amapApiService.getWeather(adcode);

        if (!weather.isSuccess()) {
            return;
        }

        String dayWeather = weather.getDayWeather() != null ? weather.getDayWeather() : "";
        String nightWeather = weather.getNightWeather() != null ? weather.getNightWeather() : "";

        if (isBadWeather(dayWeather) || isBadWeather(nightWeather)) {
            String reason = isBadWeather(dayWeather) ? dayWeather : nightWeather;
            addSurcharge(surcharges, "weather", "恶劣天气加价(" + reason + ")", WEATHER_MULTIPLIER);
        }
    }

    private boolean isBadWeather(String weather) {
        if (weather == null || weather.isEmpty()) return false;
        return BAD_WEATHER_KEYWORDS.stream().anyMatch(weather::contains);
    }

    private void addSurcharge(List<PricingResponse.SurchargeDetail> surcharges,
                              String type, String reason, double multiplier) {
        PricingResponse.SurchargeDetail detail = new PricingResponse.SurchargeDetail();
        detail.setType(type);
        detail.setReason(reason);
        detail.setMultiplier(multiplier);
        detail.setAmount(multiplier);
        surcharges.add(detail);
    }

    private String inferAdcode(Double lat, Double lng) {
        return "330300";
    }
}
