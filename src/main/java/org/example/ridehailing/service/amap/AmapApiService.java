package org.example.ridehailing.service.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AmapApiService {

    @Value("${amap.api.key:}")
    private String apiKey;

    @Value("${amap.api.base-url:https://restapi.amap.com/v3}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteResult getRouteInfo(double originLng, double originLat, double destLng, double destLat) {
        RouteResult result = new RouteResult();
        if (isApiKeyMissing()) {
            log.warn("Amap API key not configured, returning default route result");
            return result;
        }

        try {
            String url = String.format(
                    "%s/direction/driving?key=%s&origin=%.6f,%.6f&destination=%.6f,%.6f&strategy=0&extensions=all",
                    baseUrl, apiKey, originLng, originLat, destLng, destLat);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Amap route API returned null response");
                return result;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap route API returned error: {}", root.path("info").asText());
                return result;
            }

            JsonNode route = root.path("route");
            JsonNode paths = route.path("paths");
            if (paths.isEmpty()) {
                log.warn("Amap route API returned no paths");
                return result;
            }

            JsonNode path = paths.get(0);
            double distanceM = path.path("distance").asDouble(0);
            double durationS = path.path("duration").asDouble(0);

            result.setDistanceKm(Math.round(distanceM / 1000.0 * 100.0) / 100.0);
            result.setDurationMinutes(Math.round(durationS / 60.0 * 10.0) / 10.0);

            List<Integer> congestionLevels = extractCongestionLevels(path);
            if (!congestionLevels.isEmpty()) {
                double avg = congestionLevels.stream().mapToInt(i -> i).average().orElse(0.0);
                result.setAvgCongestionLevel((int) Math.round(avg));
            }

            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed to call Amap route API: {}", e.getMessage());
        }

        return result;
    }

    public WeatherResult getWeather(String cityAdcode) {
        WeatherResult result = new WeatherResult();
        if (isApiKeyMissing()) {
            log.warn("Amap API key not configured, returning default weather result");
            return result;
        }

        try {
            String url = String.format("%s/weather/weatherInfo?key=%s&city=%s&extensions=all",
                    baseUrl, apiKey, cityAdcode);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("Amap weather API returned null response");
                return result;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap weather API returned error: {}", root.path("info").asText());
                return result;
            }

            JsonNode forecasts = root.path("forecasts");
            if (forecasts.isEmpty()) {
                return result;
            }

            JsonNode today = forecasts.get(0).path("casts").get(0);
            if (today == null) {
                return result;
            }

            result.setDayWeather(today.path("dayweather").asText(""));
            result.setNightWeather(today.path("nightweather").asText(""));
            result.setDayTemp(today.path("daytemp").asText(""));
            result.setNightTemp(today.path("nighttemp").asText(""));
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed to call Amap weather API: {}", e.getMessage());
        }

        return result;
    }

    private List<Integer> extractCongestionLevels(JsonNode path) {
        List<Integer> levels = new ArrayList<>();
        JsonNode steps = path.path("steps");
        for (JsonNode step : steps) {
            JsonNode tmcs = step.path("tmcs");
            for (JsonNode tmc : tmcs) {
                String status = tmc.path("status").asText("");
                int level = parseCongestionStatus(status);
                if (level >= 0) {
                    levels.add(level);
                }
            }
        }
        return levels;
    }

    private int parseCongestionStatus(String status) {
        if (status == null || status.isEmpty()) return -1;
        return switch (status) {
            case "畅通" -> 0;
            case "缓行" -> 2;
            case "拥堵" -> 3;
            case "严重拥堵" -> 4;
            default -> -1;
        };
    }

    private boolean isApiKeyMissing() {
        return apiKey == null || apiKey.trim().isEmpty();
    }

    @lombok.Data
    public static class RouteResult {
        private boolean success;
        private double distanceKm;
        private double durationMinutes;
        private int avgCongestionLevel;
    }

    @lombok.Data
    public static class WeatherResult {
        private boolean success;
        private String dayWeather;
        private String nightWeather;
        private String dayTemp;
        private String nightTemp;
    }
}
