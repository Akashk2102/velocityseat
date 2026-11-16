package com.velocityseat.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityseat.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final int SENSITIVE_LIMIT_PER_MINUTE = 15;
    private static final int GENERAL_LIMIT_PER_MINUTE = 120;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip docs and static resources
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        boolean isSensitive = path.contains("/auth/login") ||
                              path.contains("/auth/register") ||
                              path.contains("/reservations/hold") ||
                              path.contains("/bookings/confirm");

        int maxAllowed = isSensitive ? SENSITIVE_LIMIT_PER_MINUTE : GENERAL_LIMIT_PER_MINUTE;
        String bucketKey = RATE_LIMIT_PREFIX + (isSensitive ? "strict:" : "general:") + clientIp;

        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(bucketKey);

            if (currentCount != null && currentCount == 1) {
                // First request in this minute window: set 60-second TTL
                stringRedisTemplate.expire(bucketKey, Duration.ofSeconds(60));
            }

            if (currentCount != null && currentCount > maxAllowed) {
                log.warn("Rate limit exceeded for IP {} on path {}. Current count: {}", clientIp, path, currentCount);
                handleRateLimitExceeded(response);
                return;
            }
        } catch (Exception ex) {
            // In case of Redis outage, fail open so legitimate user requests are not blocked
            log.error("Rate limiter Redis check failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Too many requests. You have exceeded the rate limit. Please wait a minute before retrying."
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
