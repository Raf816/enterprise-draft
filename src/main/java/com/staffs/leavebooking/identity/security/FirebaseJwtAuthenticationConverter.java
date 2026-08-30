package com.staffs.leavebooking.identity.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Converts Firebase JWT tokens into Spring Security authentication tokens
 * by extracting the "role" custom claim (Lecture 9 — JWT, RBAC).
 *
 * <p><strong>How it fits in the security chain:</strong>
 * <pre>
 * HTTP request with Bearer token
 *   → SecurityConfig's OAuth2 Resource Server validates the JWT signature
 *   → JwtDecoder (from FirebaseConfig) decodes and validates the JWT
 *   → THIS converter extracts the "role" custom claim
 *   → Creates a JwtAuthenticationToken with ROLE_xxx authority
 *   → SecurityContextHolder is populated with the authenticated user
 *   → @PreAuthorize("hasRole('ADMIN')") can now check the authority
 * </pre>
 *
 * <p><strong>Firebase custom claims:</strong> When a user is registered, their role
 * is stored as a custom claim in Firebase:
 * {@code firebaseAuth.setCustomUserClaims(uid, Map.of("role", "ADMIN"))}
 * This claim appears in every JWT the user receives after login.
 *
 * <p><strong>Default role:</strong> If the JWT has no "role" claim (e.g., legacy users),
 * the converter defaults to ROLE_STAFF to prevent unauthenticated access.
 *
 * <p><strong>Used by:</strong> {@code SecurityConfig.filterChain()} configures the
 * OAuth2 resource server to use this converter:
 * {@code .jwt(jwt -> jwt.jwtAuthenticationConverter(this))}
 *
 * @see SecurityConfig where this converter is wired into the security filter chain
 * @see Role for the enum defining valid roles and the ROLE_ prefix
 */
@Component // Spring-managed singleton — injected into SecurityConfig
public class FirebaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converts a decoded JWT into a Spring Security authentication token.
     *
     * <p><strong>Steps:</strong>
     * <ol>
     *   <li>Extract the "role" claim from the JWT payload</li>
     *   <li>Map it to a Spring Security authority with the ROLE_ prefix</li>
     *   <li>Create a JwtAuthenticationToken with the authority and the user's UID as principal</li>
     * </ol>
     *
     * @param jwt the decoded and validated JWT from Firebase
     * @return a JwtAuthenticationToken with the user's role as a granted authority
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract the "role" custom claim from the JWT payload
        // This was set during registration: firebaseAuth.setCustomUserClaims(uid, Map.of("role", "ADMIN"))
        String roleClaim = jwt.getClaimAsString("role");

        // Map the role claim to a Spring Security authority string
        // If the claim is null or blank, default to ROLE_STAFF (principle of least privilege)
        String authority = (roleClaim != null && !roleClaim.isBlank())
                ? Role.PREFIX + roleClaim.toUpperCase()   // e.g., "ROLE_" + "ADMIN" = "ROLE_ADMIN"
                : Role.STAFF.getAuthority();              // Default: "ROLE_STAFF"

        // Create a single-element list of granted authorities
        // (each user has exactly one role in this system)
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(authority)  // e.g., SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // Create and return the authentication token
        // Parameters: jwt (credentials), authorities (what they can do), principal name (who they are)
        // jwt.getSubject() returns the Firebase UID — this becomes authentication.getName()
        return new JwtAuthenticationToken(
                jwt,                                       // The original JWT (for claim access)
                authorities,                               // The user's role-based authority
                Objects.requireNonNull(jwt.getSubject())   // Firebase UID as the principal name
        );
    }
}
