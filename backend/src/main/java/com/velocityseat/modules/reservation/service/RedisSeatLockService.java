package com.velocityseat.modules.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityseat.modules.reservation.dto.ReservationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSeatLockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SEAT_HOLD_KEY_PREFIX = "seat:hold:";
    private static final String RESERVATION_PAYLOAD_PREFIX = "reservation:payload:";

    // Lua script: safely delete key ONLY if its current value matches the token
    private static final String RELEASE_LOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * Atomically attempts to hold all requested seats.
     * If any seat is already locked by another user, releases all already acquired seats and returns false.
     */
    public boolean acquireSeatHolds(Long eventId, List<Long> seatIds, String reservationToken, long ttlSeconds) {
        List<String> successfullyAcquiredKeys = new ArrayList<>();
        // Sort seat IDs to guarantee strict lock ordering across all concurrent threads
        List<Long> sortedSeatIds = seatIds.stream().sorted().toList();

        for (Long seatId : sortedSeatIds) {
            String lockKey = buildSeatKey(eventId, seatId);
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, reservationToken, Duration.ofSeconds(ttlSeconds));

            if (Boolean.TRUE.equals(acquired)) {
                successfullyAcquiredKeys.add(lockKey);
            } else {
                // Conflict detected: Another client beat us to this seat!
                log.warn("Failed to acquire lock for key: {}. Rolling back previously acquired locks.", lockKey);
                rollbackAcquiredKeys(successfullyAcquiredKeys, reservationToken);
                return false;
            }
        }

        return true;
    }

    public void saveReservationPayload(String reservationToken, ReservationPayload payload, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue()
                    .set(RESERVATION_PAYLOAD_PREFIX + reservationToken, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to serialize reservation payload for token: {}", reservationToken, e);
            throw new RuntimeException("Error storing reservation in cache", e);
        }
    }

    public ReservationPayload getReservationPayload(String reservationToken) {
        try {
            String json = stringRedisTemplate.opsForValue().get(RESERVATION_PAYLOAD_PREFIX + reservationToken);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, ReservationPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse reservation payload for token: {}", reservationToken, e);
            return null;
        }
    }

    public void releaseSeatHolds(Long eventId, List<Long> seatIds, String reservationToken) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(RELEASE_LOCK_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        for (Long seatId : seatIds) {
            String key = buildSeatKey(eventId, seatId);
            stringRedisTemplate.execute(redisScript, Collections.singletonList(key), reservationToken);
        }

        stringRedisTemplate.delete(RESERVATION_PAYLOAD_PREFIX + reservationToken);
    }

    public boolean isSeatHeldInRedis(Long eventId, Long seatId) {
        String key = buildSeatKey(eventId, seatId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private void rollbackAcquiredKeys(List<String> keys, String reservationToken) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(RELEASE_LOCK_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        for (String key : keys) {
            stringRedisTemplate.execute(redisScript, Collections.singletonList(key), reservationToken);
        }
    }

    private String buildSeatKey(Long eventId, Long seatId) {
        return SEAT_HOLD_KEY_PREFIX + eventId + ":" + seatId;
    }
}
