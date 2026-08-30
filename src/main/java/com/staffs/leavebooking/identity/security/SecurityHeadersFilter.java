package com.staffs.leavebooking.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter that obfuscates server version information and adds
 * standard security headers to all HTTP responses (Lecture 9 — Security Headers).
 *
 * <p><strong>Brief requirement:</strong> "The system will adjust its HTTP headers
 * so that the server version is obfuscated."
 *
 * <p><strong>Headers applied:</strong>
 * <table>
 *   <tr><th>Header</th><th>Value</th><th>Purpose</th></tr>
 *   <tr><td>Server</td><td>(blank)</td><td>Hides Tomcat/Spring version from attackers</td></tr>
 *   <tr><td>X-Powered-By</td><td>(blank)</td><td>Hides technology stack</td></tr>
 *   <tr><td>X-Content-Type-Options</td><td>nosniff</td><td>Prevents MIME-type sniffing attacks</td></tr>
 *   <tr><td>X-Frame-Options</td><td>DENY / SAMEORIGIN</td><td>Prevents clickjacking (SAMEORIGIN for H2 console)</td></tr>
 *   <tr><td>Strict-Transport-Security</td><td>max-age=31536000</td><td>Enforces HTTPS for 1 year (HSTS)</td></tr>
 *   <tr><td>Cache-Control</td><td>no-store</td><td>Prevents browser caching of API responses</td></tr>
 *   <tr><td>X-XSS-Protection</td><td>0</td><td>Disabled (modern browsers use CSP instead)</td></tr>
 * </table>
 *
 * <p><strong>Why X-XSS-Protection: 0?</strong> The old X-XSS-Protection: 1 header
 * had security vulnerabilities in some browsers. Modern best practice is to disable it
 * and use Content-Security-Policy (CSP) headers instead. Setting to 0 explicitly
 * disables it to avoid the buggy browser implementations.
 *
 * <p><strong>Filter chain position:</strong> Runs AFTER SecurityContextHolder setup
 * (configured in SecurityConfig via addFilterAfter). This ensures the security
 * context is available if any header logic needs it (though currently it doesn't).
 *
 * @see SecurityConfig where this filter is added to the security filter chain
 */
@Component // Spring-managed singleton — injected into SecurityConfig
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * Adds security headers to every HTTP response.
     * Runs once per request (OncePerRequestFilter guarantees this).
     *
     * @param request     the incoming HTTP request (used to check URI for H2 console)
     * @param response    the outgoing HTTP response (headers are set here)
     * @param filterChain the remaining filters to execute after adding headers
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Server identity obfuscation ──
        // Set Server header to blank — hides "Apache Tomcat/10.x" from responses
        response.setHeader("Server", "");
        // Remove X-Powered-By — hides "Spring Boot" or framework details
        response.setHeader("X-Powered-By", "");

        // ── Standard security headers ──

        // Prevent browsers from guessing the MIME type of responses
        // (prevents "content sniffing" attacks where a browser treats a file as executable)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent the page from being embedded in iframes (clickjacking protection)
        // Exception: H2 console uses iframes for its UI, so allow SAMEORIGIN there
        if (request.getRequestURI().startsWith("/h2-console")) {
            response.setHeader("X-Frame-Options", "SAMEORIGIN"); // Allow H2 console frames
        } else {
            response.setHeader("X-Frame-Options", "DENY"); // Block all framing for API endpoints
        }

        // HTTP Strict Transport Security — tells browsers to only use HTTPS for 1 year
        // includeSubDomains applies the rule to all subdomains too
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Prevent browsers from caching API responses (sensitive data like tokens, leave details)
        response.setHeader("Cache-Control", "no-store");

        // Explicitly disable the XSS filter (buggy in old browsers, CSP is the modern replacement)
        response.setHeader("X-XSS-Protection", "0");

        // Continue the filter chain — pass the request to the next filter
        filterChain.doFilter(request, response);
    }
}
