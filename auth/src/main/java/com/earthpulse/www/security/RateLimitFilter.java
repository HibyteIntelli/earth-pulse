package com.earthpulse.www.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH                   = "/auth/login";
    private static final String SIGNUP_PATH                  = "/auth/signup";
    private static final String HEADER_REMAINING             = "X-Rate-Limit-Remaining";
    private static final String HEADER_RETRY_AFTER           = "X-Rate-Limit-Retry-After-Seconds";
    private static final String CONTENT_TYPE_JSON            = "application/json";
    private static final String BODY_TOO_MANY_REQUESTS       = "{\"error\":\"Too many requests\"}";

    private final int loginCapacity;
    private final int signupCapacity;
    private final Duration refillDuration;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int loginCapacity, int signupCapacity, Duration refillDuration) {
        this.loginCapacity = loginCapacity;
        this.signupCapacity = signupCapacity;
        this.refillDuration = refillDuration;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        int capacity;
        if (LOGIN_PATH.equals(path)) {
            capacity = loginCapacity;
        } else if (SIGNUP_PATH.equals(path)) {
            capacity = signupCapacity;
        } else {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, _ -> buildBucket(capacity));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfter = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            response.setStatus(429);
            response.setContentType(CONTENT_TYPE_JSON);
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfter));
            response.getWriter().write(BODY_TOO_MANY_REQUESTS);
        }
    }

    private Bucket buildBucket(int capacity) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, refillDuration))
                .build();
    }
}
