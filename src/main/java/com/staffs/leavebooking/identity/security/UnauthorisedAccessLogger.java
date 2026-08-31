package com.staffs.leavebooking.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Combined handler for 401 Unauthorized and 403 Forbidden responses
 * (Lecture 9 — Security, Audit Logging).
 *
 * <p><strong>Brief requirement:</strong> "Requests will check the role of the user
 * to establish if they are authorised to view a particular end point — a log should
 * be kept of unauthorised access to end points."
 *
 * <p><strong>Implements two Spring Security interfaces:</strong>
 * <ul>
 *   <li>{@link AuthenticationEntryPoint} — handles 401 Unauthorized (no valid JWT provided)</li>
 *   <li>{@link AccessDeniedHandler} — handles 403 Forbidden (valid JWT but insufficient role)</li>
 * </ul>
 *
 * <p><strong>What gets logged:</strong> Every unauthorized or forbidden access attempt
 * is logged with WARN level including: timestamp, HTTP method, URI, client IP,
 * authenticated principal (if any), and the reason for denial.
 *
 * <p><strong>JSON error response:</strong> Both 401 and 403 return a consistent JSON
 * body matching the {@code ErrorResponse} format used throughout the application.
 *
 * <p><strong>Wired in SecurityConfig:</strong>
 * <pre>
 * .exceptionHandling(exceptions -> exceptions
 *     .authenticationEntryPoint(unauthorisedAccessLogger)  // handles 401
 *     .accessDeniedHandler(unauthorisedAccessLogger))      // handles 403
 * </pre>
 *
 * @see SecurityConfig where this handler is wired into the security filter chain
 */
@Component // Spring-managed singleton — injected into SecurityConfig
@Slf4j     // Lombok: generates a private static final Logger
public class UnauthorisedAccessLogger implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** Jackson ObjectMapper for writing JSON error responses to the output stream */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles 401 Unauthorized — called when no valid authentication is provided.
     * This happens when: no Bearer token in the request, token is expired,
     * token has invalid signature, or token is malformed.
     *
     * @param request       the HTTP request that triggered the 401
     * @param response      the HTTP response to write the error to
     * @param authException the authentication exception with details of why auth failed
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Extract request details for the audit log
        String clientIp = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Log the unauthorised access attempt (WARN level for security monitoring)
        log.warn("UNAUTHORISED ACCESS [401] | IP: {} | {} {} | Reason: {}",
                clientIp, method, uri, authException.getMessage());

        // Return a JSON error response to the client
        writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                "Authentication required. Please provide a valid Bearer token.");
    }

    /**
     * Handles 403 Forbidden — called when the user IS authenticated but doesn't
     * have the required role/authority for the requested endpoint.
     * This happens when: a STAFF user tries to access an ADMIN-only endpoint,
     * a MANAGER tries to approve a request not assigned to them, etc.
     *
     * @param request               the HTTP request that triggered the 403
     * @param response              the HTTP response to write the error to
     * @param accessDeniedException the exception with details of why access was denied
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // Extract request details for the audit log
        String clientIp = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        // Get the authenticated user's principal name (Firebase UID), or "anonymous" if not set
        String principal = (request.getUserPrincipal() != null)
                ? request.getUserPrincipal().getName()
                : "anonymous";

        // Log the forbidden access attempt with the user's identity
        log.warn("FORBIDDEN ACCESS [403] | IP: {} | User: {} | {} {} | Reason: {}",
                clientIp, principal, method, uri, accessDeniedException.getMessage());

        // Return a JSON error response to the client
        writeErrorResponse(response, HttpStatus.FORBIDDEN,
                "Access denied. You do not have permission to access this resource.");
    }

    /**
     * Writes a JSON error response to the HTTP output stream.
     * Uses the same format as GlobalExceptionHandler and AuthController errors
     * for consistency across the entire API.
     *
     * @param response the HTTP response to write to
     * @param status   the HTTP status (401 or 403)
     * @param message  the human-readable error message
     */
    private void writeErrorResponse(HttpServletResponse response, HttpStatus status,
                                    String message) throws IOException {
        // Set the HTTP status code
        response.setStatus(status.value());
        // Set Content-Type to JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Build the error response body matching our standard format
        Map<String, Object> body = Map.of(
                "status", status.value(),           // e.g., 401 or 403
                "error", status.getReasonPhrase(),  // e.g., "Unauthorized" or "Forbidden"
                "message", message,                 // Human-readable error description
                "timestamp", Instant.now().toString() // ISO-8601 timestamp
        );

        // Serialize the Map to JSON and write to the response output stream
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Extracts the client's real IP address from the request.
     * Respects X-Forwarded-For header from reverse proxies.
     *
     * @param request the HTTP request
     * @return the client's IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
