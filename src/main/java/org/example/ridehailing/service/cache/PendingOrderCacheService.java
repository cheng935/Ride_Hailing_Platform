package org.example.ridehailing.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridehailing.model.order.Order;
import org.example.ridehailing.model.order.OrderStatus;
import org.example.ridehailing.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingOrderCacheService {

    private static final String PENDING_ORDER_KEY = "pending_orders";
    private static final String ORDER_DETAIL_PREFIX = "order:detail:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public void addPendingOrder(Order order) {
        try {
            Map<String, Object> orderMap = orderToMap(order);
            String json = objectMapper.writeValueAsString(orderMap);
            redisTemplate.opsForHash().put(PENDING_ORDER_KEY, String.valueOf(order.getOrderId()), json);
            redisTemplate.expire(PENDING_ORDER_KEY, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Added pending order {} to Redis cache", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to add pending order to Redis: {}", e.getMessage());
        }
    }

    public void removePendingOrder(Long orderId) {
        try {
            redisTemplate.opsForHash().delete(PENDING_ORDER_KEY, String.valueOf(orderId));
            log.info("Removed pending order {} from Redis cache", orderId);
        } catch (Exception e) {
            log.error("Failed to remove pending order from Redis: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getPendingOrders() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(PENDING_ORDER_KEY);
            if (entries != null && !entries.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object value : entries.values()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = objectMapper.readValue((String) value, Map.class);
                    result.add(map);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to get pending orders from Redis, falling back to DB: {}", e.getMessage());
        }

        return refreshFromDatabase();
    }

    public List<Map<String, Object>> refreshFromDatabase() {
        List<Order> orders = orderRepository.findByStatusIn(List.of(OrderStatus.PENDING));
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            redisTemplate.delete(PENDING_ORDER_KEY);
            for (Order order : orders) {
                Map<String, Object> orderMap = orderToMap(order);
                String json = objectMapper.writeValueAsString(orderMap);
                redisTemplate.opsForHash().put(PENDING_ORDER_KEY, String.valueOf(order.getOrderId()), json);
                result.add(orderMap);
            }
            if (!result.isEmpty()) {
                redisTemplate.expire(PENDING_ORDER_KEY, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("Failed to refresh pending orders cache: {}", e.getMessage());
            for (Order order : orders) {
                result.add(orderToMap(order));
            }
        }

        return result;
    }

    public void cacheOrderDetail(Order order) {
        try {
            Map<String, Object> orderMap = orderToDetailMap(order);
            String json = objectMapper.writeValueAsString(orderMap);
            redisTemplate.opsForValue().set(ORDER_DETAIL_PREFIX + order.getOrderId(), json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to cache order detail: {}", e.getMessage());
        }
    }

    public void evictOrderDetail(Long orderId) {
        redisTemplate.delete(ORDER_DETAIL_PREFIX + orderId);
    }

    private Map<String, Object> orderToMap(Order o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("passengerName", o.getPassenger() != null ? o.getPassenger().getName() : null);
        m.put("passengerPhone", o.getPassenger() != null ? o.getPassenger().getPhone() : null);
        m.put("pickupName", o.getPickupLocation());
        m.put("pickupLat", o.getPickupLat());
        m.put("pickupLng", o.getPickupLng());
        m.put("destName", o.getDestination());
        m.put("destLat", o.getDestLat());
        m.put("destLng", o.getDestLng());
        m.put("distance", o.getDistance());
        m.put("estimatedFare", o.getEstimatedFare());
        m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
        return m;
    }

    private Map<String, Object> orderToDetailMap(Order o) {
        Map<String, Object> m = orderToMap(o);
        m.put("status", o.getStatus().name());
        m.put("driverName", o.getDriver() != null ? o.getDriver().getName() : null);
        m.put("driverId", o.getDriver() != null ? o.getDriver().getUserId() : null);
        m.put("driverPhone", o.getDriver() != null ? o.getDriver().getPhone() : null);
        m.put("vehiclePlate", o.getDriver() != null ? o.getDriver().getVehiclePlate() : null);
        m.put("actualFare", o.getActualFare());
        m.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID");
        return m;
    }
}
