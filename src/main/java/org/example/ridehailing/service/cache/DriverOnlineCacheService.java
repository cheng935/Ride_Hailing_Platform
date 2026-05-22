package org.example.ridehailing.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverOnlineCacheService {

    private static final String ONLINE_DRIVERS_KEY = "drivers:online";

    private final StringRedisTemplate redisTemplate;

    public void driverOnline(Long driverId) {
        redisTemplate.opsForSet().add(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        log.info("Driver {} is now online (Redis)", driverId);
    }

    public void driverOffline(Long driverId) {
        redisTemplate.opsForSet().remove(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        log.info("Driver {} is now offline (Redis)", driverId);
    }

    public boolean isDriverOnline(Long driverId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        return Boolean.TRUE.equals(isMember);
    }

    public Set<String> getOnlineDriverIds() {
        return redisTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
    }

    public long getOnlineDriverCount() {
        Long count = redisTemplate.opsForSet().size(ONLINE_DRIVERS_KEY);
        return count != null ? count : 0;
    }
}
