package com.staffs.leavebooking.identity.security;

import com.staffs.leavebooking.identity.authService.FirebaseTokenFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central security configuration for the application (Lecture 9 — Spring Security, JWT, RBAC).
 *
 * <p><strong>Key decisions:</strong>
 * <ul>
 *   <li><strong>CSRF disabled:</strong> The API is stateless (JWT-based auth, no cookies/sessions).
 *       CSRF protection is unnecessary and would interfere with API clients like Postman.</li>
 *   <li><strong>/auth/** is public:</strong> Registration and login must be accessible without
 *       a JWT token (chicken-and-egg problem — you need to register/login to get a token).</li>
 *   <li><strong>Everything else requires authentication:</strong> All business endpoints
 *       require a valid Bearer token in the Authorization header.</li>
 *   <li><strong>@EnableMethodSecurity:</strong> Enables {@code @PreAuthorize} annotations
 *       on facade methods for fine-grained RBAC (role-based access control).</li>
 * </ul>
 *
 * <p><strong>Filter chain order:</strong>
 * <pre>
 * Request → SecurityHeadersFilter → RateLimitFilter → FirebaseTokenFilter
 *         → OAuth2 JWT validation → @PreAuthorize check → Controller
 * </pre>
 *
 * <p><strong>Profile: !test</strong> — This config is NOT loaded during tests.
 * Tests use {@code TestSecurityConfig} which permits all requests and mocks Firebase.
 *
 * @see FirebaseTokenFilter for JWT verification and SecurityContext population
 * @see FirebaseJwtAuthenticationConverter for JWT role → Spring authority mapping
 * @see RateLimitFilter for brute-force protection on login
 * @see SecurityHeadersFilter for HTTP security headers
 * @see UnauthorisedAccessLogger for 401/403 logging and responses
 */
@Configuration           // Spring configuration class — provides bean definitions
@EnableWebSecurity       // Activates Spring Security's web security support
@EnableMethodSecurity    // Enables @PreAuthorize, @PostAuthorize on methods
@AllArgsConstructor      // Lombok: constructor injection for all final fields
@org.springframework.context.annotation.Profile("!test") // Skip in test profile
public class SecurityConfig {

    /** Converts Firebase JWTs into Spring Security authentication tokens with role authorities */
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    /** Pre-security filter that verifies Firebase ID tokens and populates SecurityContext */
    private final FirebaseTokenFilter firebaseTokenFilter;

    /** Rate limiting filter — limits login attempts per IP (brute-force protection) */
    private final RateLimitFilter rateLimitFilter;

    /** Adds security headers (Server obfuscation, X-Frame-Options, etc.) to all responses */
    private final SecurityHeadersFilter securityHeadersFilter;

    /** Combined 401/403 handler — logs unauthorized access and returns JSON error responses */
    private final UnauthorisedAccessLogger unauthorisedAccessLogger;

    /**
     * Configures the HTTP security filter chain.
     * This is the main security configuration method — defines what's public,
     * what requires auth, and how authentication/authorization works.
     *
     * @param http the HttpSecurity builder provided by Spring
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF disabled — stateless JWT auth (no cookies/sessions, so CSRF is irrelevant)
                .csrf(AbstractHttpConfigurer::disable)

                // URL-level authorization rules
                .authorizeHttpRequests(auth -> auth
                        // /auth/** endpoints are public (register, login)
                        .requestMatchers("/auth/**").permitAll()
                        // H2 console is public (development tool — would be disabled in production)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Everything else requires a valid JWT token
                        .anyRequest().authenticated()
                )

                // OAuth2 Resource Server — validates JWTs and converts them to Authentication objects
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        // Uses FirebaseJwtAuthenticationConverter to extract role claims
                )

                // Custom exception handling — use our logger for 401 and 403 responses
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorisedAccessLogger)  // Handles 401
                        .accessDeniedHandler(unauthorisedAccessLogger)       // Handles 403
                )

                // Custom filter ordering:
                // RateLimitFilter runs BEFORE authentication (blocks brute-force before JWT check)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                // FirebaseTokenFilter runs BEFORE authentication (pre-populates SecurityContext)
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // SecurityHeadersFilter runs AFTER SecurityContextHolder is set up
                .addFilterAfter(securityHeadersFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)

                // Allow H2 console iframes (dev tool — uses frames for its UI)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                .build();
    }
}
