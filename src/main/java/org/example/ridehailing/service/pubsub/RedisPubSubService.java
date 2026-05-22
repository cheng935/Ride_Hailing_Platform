package org.example.ridehailing.service.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridehailing.websocket.RideWebSocketHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPubSubService {

    private static final String ORDER_CHANNEL = "channel:order";
    private static final String DRIVER_CHANNEL = "channel:driver";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final RideWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener((message, pattern) -> {
            try {
                String body = new String(message.getBody());
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = objectMapper.readValue(body, Map.class);
                String channel = new String(message.getChannel());

                if (ORDER_CHANNEL.equals(channel)) {
                    handleOrderMessage(msg);
                } else if (DRIVER_CHANNEL.equals(channel)) {
                    handleDriverMessage(msg);
                }
            } catch (Exception e) {
                log.error("Failed to handle Redis message: {}", e.getMessage());
            }
        }, new ChannelTopic(ORDER_CHANNEL));

        listenerContainer.addMessageListener((message, pattern) -> {
            try {
                String body = new String(message.getBody());
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = objectMapper.readValue(body, Map.class);
                handleDriverMessage(msg);
            } catch (Exception e) {
                log.error("Failed to handle driver Redis message: {}", e.getMessage());
            }
        }, new ChannelTopic(DRIVER_CHANNEL));
    }

    public void publishOrderEvent(String eventType, Long orderId, Long passengerId, Long driverId, Map<String, Object> extra) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "order");
            message.put("event", eventType);
            message.put("orderId", orderId);
            message.put("passengerId", passengerId);
            message.put("driverId", driverId);
            message.put("timestamp", System.currentTimeMillis());
            if (extra != null) message.putAll(extra);

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(ORDER_CHANNEL, json);
            log.info("Published order event: {} for order {}", eventType, orderId);
        } catch (Exception e) {
            log.error("Failed to publish order event: {}", e.getMessage());
        }
    }

    public void publishDriverEvent(String eventType, Long driverId, Map<String, Object> extra) {
        try {
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "driver");
            message.put("event", eventType);
            message.put("driverId", driverId);
            message.put("timestamp", System.currentTimeMillis());
            if (extra != null) message.putAll(extra);

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(DRIVER_CHANNEL, json);
            log.info("Published driver event: {} for driver {}", eventType, driverId);
        } catch (Exception e) {
            log.error("Failed to publish driver event: {}", e.getMessage());
        }
    }

    private void handleOrderMessage(Map<String, Object> msg) {
        try {
            String event = (String) msg.get("event");
            String json = objectMapper.writeValueAsString(msg);

            Long passengerId = msg.get("passengerId") != null ? ((Number) msg.get("passengerId")).longValue() : null;
            Long driverId = msg.get("driverId") != null ? ((Number) msg.get("driverId")).longValue() : null;

            if (passengerId != null) {
                webSocketHandler.sendToUser(passengerId, json);
            }
            if (driverId != null) {
                webSocketHandler.sendToUser(driverId, json);
            }

            if ("CREATED".equals(event)) {
                webSocketHandler.broadcastToDrivers(json);
            }
        } catch (Exception e) {
            log.error("Failed to handle order message: {}", e.getMessage());
        }
    }

    private void handleDriverMessage(Map<String, Object> msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            webSocketHandler.broadcastToDrivers(json);
        } catch (Exception e) {
            log.error("Failed to handle driver message: {}", e.getMessage());
        }
    }
}
