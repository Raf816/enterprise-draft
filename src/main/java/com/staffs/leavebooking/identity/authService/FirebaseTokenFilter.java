package com.staffs.leavebooking.identity.authService;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.staffs.leavebooking.identity.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Pre-security-chain filter that verifies Firebase ID tokens (JWTs) and populates
 * the Spring Security context (Lecture 9 — JWT Authentication, Filter Chain).
 *
 * <p><strong>How it works in the security chain:</strong>
 * <pre>
 * HTTP Request arrives
 *   → RateLimitFilter (blocks brute-force on /auth/login)
 *   → THIS FILTER (verifies JWT, populates SecurityContext)
 *   → OAuth2 Resource Server (additional JWT validation)
 *   → @PreAuthorize checks (role-based access control)
 *   → Controller method executes
 * </pre>
 *
 * <p><strong>Token verification:</strong> Uses the Firebase Admin SDK's
 * {@code verifyIdToken()} method, which:
 * <ul>
 *   <li>Verifies the JWT's cryptographic signature against Google's public keys</li>
 *   <li>Checks the token hasn't expired</li>
 *   <li>Checks the issuer and audience claims match the Firebase project</li>
 *   <li>Returns a {@code FirebaseToken} with the decoded claims (UID, email, role, etc.)</li>
 * </ul>
 *
 * <p><strong>OncePerRequestFilter:</strong> Guarantees this filter runs exactly once
 * per request, even if the request is internally forwarded (e.g., error handling).
 *
 * <p><strong>Why both this filter AND FirebaseJwtAuthenticationConverter?</strong>
 * This filter handles the Firebase Admin SDK verification path (for direct token verification).
 * The JwtAuthenticationConverter handles the Spring OAuth2 Resource Server path
 * (for JWK-based verification). Both populate the SecurityContext — whichever runs
 * first "wins" for that request.
 *
 * @see SecurityConfig where this filter is added to the chain
 * @see com.staffs.leavebooking.identity.security.FirebaseJwtAuthenticationConverter for the OAuth2 RS path
 */
@Component // Spring-managed singleton — injected into SecurityConfig
@Slf4j     // Lombok: generates a private static final Logger
public class FirebaseTokenFilter extends OncePerRequestFilter {

    /** The "Bearer " prefix in the Authorization header (note the trailing space) */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Main filter method — extracts and verifies the Bearer token from the Authorization header.
     * If valid, populates the SecurityContext with the authenticated user's details.
     * If invalid, returns 401 Unauthorized.
     * If no token present, passes through to the next filter (may be a public endpoint).
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response (used for 401 on verification failure)
     * @param filterChain the remaining filters to execute after this one
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the Authorization header from the request
        final String authHeader = request.getHeader("Authorization");

        // Only process if there's a Bearer token present
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            // Extract the token string (everything after "Bearer ")
            final String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                // Verify the token using Firebase Admin SDK
                // This checks: signature (using Google's public keys), expiration, issuer, audience
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

                // Extract the Firebase UID (this becomes authentication.getName() in controllers)
                String uid = decodedToken.getUid();

                // Extract the "role" custom claim that was set during registration
                // Custom claims are stored in the JWT payload by Firebase
                String roleClaim = (String) decodedToken.getClaims().get("role");

                // Map the role claim to a Spring Security authority
                // Default to ROLE_STAFF if no role claim is present (principle of least privilege)
                String authority = (roleClaim != null && !roleClaim.isBlank())
                        ? Role.PREFIX + roleClaim.toUpperCase()  // e.g., "ROLE_ADMIN"
                        : Role.STAFF.getAuthority();             // Default: "ROLE_STAFF"

                // Create a list of granted authorities (one role per user)
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(authority)
                );

                // Create an authenticated token with the UID as principal and role as authority
                // The three-arg constructor sets authenticated = true automatically
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(uid, null, authorities);
                //    principal = uid (Firebase UID)
                //    credentials = null (we don't need the password after verification)
                //    authorities = [ROLE_ADMIN] (the user's role)

                // Populate the SecurityContext — this is what @PreAuthorize and
                // authentication.getName() read in controllers and facade methods
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (FirebaseAuthException e) {
                // Token verification failed — invalid signature, expired, malformed, etc.
                log.warn("Firebase token verification failed: {}", e.getMessage());
                // Clear any existing security context (defensive)
                SecurityContextHolder.clearContext();
                // Return 401 Unauthorized with a clean JSON error body
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or expired authentication token. Please login again.\"}");
                return;
            }
        }
        // If no Bearer token was present, pass through (might be a public endpoint like /auth/login)

        // Continue the filter chain — pass to the next filter
        filterChain.doFilter(request, response);
    }
}
