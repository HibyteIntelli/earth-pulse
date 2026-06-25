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

    private final int loginCapacity;
    private final int signupCapacity;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int loginCapacity, int signupCapacity) {
        this.loginCapacity = loginCapacity;
        this.signupCapacity = signupCapacity;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        int capacity;
        if ("/auth/login".equals(path)) {
            capacity = loginCapacity;
        } else if ("/auth/signup".equals(path)) {
            capacity = signupCapacity;
        } else {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(capacity));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfter = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfter));
            response.getWriter().write("{\"error\":\"Too many requests\"}");
        }
    }

    private Bucket buildBucket(int capacity) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, Duration.ofMinutes(1)))
                .build();
    }


}
