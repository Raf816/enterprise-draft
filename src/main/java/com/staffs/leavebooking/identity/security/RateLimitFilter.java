package com.staffs.leavebooking.identity.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using the Bucket4j token-bucket algorithm
 * (Lecture 9 — Security, Brute-Force Protection).
 *
 * <p><strong>Brief requirement:</strong> "The system will limit the number of requests
 * from a specific end point." This filter limits each IP address to 20 login attempts
 * per minute on the POST /auth/login endpoint.
 *
 * <p><strong>Token bucket algorithm:</strong> Each IP gets a "bucket" with 20 tokens.
 * Each request consumes one token. Tokens refill at a rate of 20 per minute.
 * When the bucket is empty (20 requests in under a minute), the 21st request
 * is rejected with 429 Too Many Requests.
 *
 * <p><strong>Why only /auth/login?</strong> Login is the most common target for
 * brute-force attacks. Other endpoints already require JWT authentication,
 * making brute-force impractical.
 *
 * <p><strong>Storage:</strong> Per-IP buckets are stored in a {@link ConcurrentHashMap}
 * (thread-safe, in-memory). In a production distributed system, this would use
 * Redis or similar for shared state across multiple application instances.
 *
 * <p><strong>Filter chain position:</strong> Runs BEFORE authentication
 * (configured in SecurityConfig) so rate-limited requests are rejected
 * before the JWT validation overhead.
 *
 * @see SecurityConfig where this filter is added to the security filter chain
 */
@Component // Spring-managed singleton — injected into SecurityConfig
@Slf4j     // Lombok: generates a private static final Logger
public class RateLimitFilter extends OncePerRequestFilter {

    /** Maximum number of login requests allowed per IP per refill period */
    private static final int MAX_REQUESTS = 20;

    /** How often the bucket refills to full capacity */
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    /** Only apply rate limiting to this path */
    private static final String RATE_LIMITED_PATH = "/auth/login";

    /**
     * Thread-safe map of IP address → token bucket.
     * Each IP gets its own bucket, created on first request.
     * ConcurrentHashMap ensures thread safety for concurrent requests.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Main filter method — runs once per HTTP request.
     * Checks if the request is a login attempt, and if so, applies rate limiting.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filters to execute if not rate-limited
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit the login endpoint — all other paths pass through immediately
        if (!path.equals(RATE_LIMITED_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only rate-limit POST requests (not OPTIONS/preflight from CORS)
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the client's IP address (respecting X-Forwarded-For from proxies)
        String clientIp = getClientIp(request);

        // Get or create a token bucket for this IP address
        // computeIfAbsent is atomic — only creates the bucket once per IP
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createNewBucket);

        // Try to consume one token from the bucket
        if (bucket.tryConsume(1)) {
            // Token consumed — allow the request through to the next filter
            filterChain.doFilter(request, response);
        } else {
            // Bucket empty — rate limit exceeded, return 429 Too Many Requests
            log.warn("Rate limit exceeded for IP {} on {}", clientIp, RATE_LIMITED_PATH);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // Write a JSON error response matching the standard ErrorResponse format
            response.getWriter().write("""
                    {
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded. Maximum %d login attempts per minute. Please try again later.",
                        "timestamp": "%s"
                    }
                    """.formatted(MAX_REQUESTS, Instant.now().toString()));
        }
    }

    /**
     * Creates a new token bucket for a given IP address.
     * The bucket starts full (20 tokens) and refills completely every minute.
     *
     * <p><strong>Bandwidth.classic:</strong> "greedy" refill means all 20 tokens
     * are restored at once every minute (not gradually 1 per 3 seconds).
     *
     * @param key the IP address (unused in bucket creation, but required by computeIfAbsent)
     * @return a new Bucket configured with the rate limit
     */
    private Bucket createNewBucket(String key) {
        // Create a bandwidth limit: 20 requests per 1 minute, greedy refill
        Bandwidth limit = Bandwidth.classic(MAX_REQUESTS, Refill.greedy(MAX_REQUESTS, REFILL_DURATION));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extracts the client's real IP address from the request.
     * Respects the X-Forwarded-For header (set by reverse proxies/load balancers).
     * Falls back to the direct remote address if no proxy header is present.
     *
     * @param request the HTTP request
     * @return the client's IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For may contain multiple IPs: "client, proxy1, proxy2"
        // The first IP is the original client
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim(); // Take the first (original client) IP
        }
        return request.getRemoteAddr(); // Direct connection — no proxy
    }
}
