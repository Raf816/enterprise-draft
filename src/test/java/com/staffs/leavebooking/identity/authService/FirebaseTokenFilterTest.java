package com.staffs.leavebooking.identity.authService;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("FirebaseTokenFilter (JWT Verification)")
class FirebaseTokenFilterTest {

    private FirebaseTokenFilter filter;
    private FilterChain filterChain;
    private MockedStatic<FirebaseAuth> firebaseAuthStatic;
    private FirebaseAuth mockFirebaseAuth;

    @BeforeEach
    void setUp() {
        filter = new FirebaseTokenFilter();
        filterChain = mock(FilterChain.class);
        mockFirebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        firebaseAuthStatic.close();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("No Authorization header")
    class NoAuthHeader {

        @Test
        @DisplayName("Should pass through without authentication when no header present")
        void shouldPassThroughWithoutHeader() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should pass through when Authorization header is not Bearer")
        void shouldPassThroughWhenNotBearer() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valid Bearer token")
    class ValidToken {

        @Test
        @DisplayName("Should set authentication in SecurityContext with correct UID and role")
        void shouldSetAuthenticationOnValidToken() throws ServletException, IOException, FirebaseAuthException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            request.addHeader("Authorization", "Bearer valid-token-123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn("uid-staff-001");
            when(mockToken.getClaims()).thenReturn(Map.of("role", "STAFF"));
            when(mockFirebaseAuth.verifyIdToken("valid-token-123")).thenReturn(mockToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("uid-staff-001");
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_STAFF");
        }

        @Test
        @DisplayName("Should default to ROLE_STAFF when no role claim present")
        void shouldDefaultToStaffWhenNoRoleClaim() throws ServletException, IOException, FirebaseAuthException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            request.addHeader("Authorization", "Bearer token-no-role");
            MockHttpServletResponse response = new MockHttpServletResponse();

            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn("uid-new-user");
            when(mockToken.getClaims()).thenReturn(Map.of()); // no role claim
            when(mockFirebaseAuth.verifyIdToken("token-no-role")).thenReturn(mockToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_STAFF");
        }

        @Test
        @DisplayName("Should handle ADMIN role correctly")
        void shouldHandleAdminRole() throws ServletException, IOException, FirebaseAuthException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/staff");
            request.addHeader("Authorization", "Bearer admin-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn("uid-admin-001");
            when(mockToken.getClaims()).thenReturn(Map.of("role", "ADMIN"));
            when(mockFirebaseAuth.verifyIdToken("admin-token")).thenReturn(mockToken);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Invalid Bearer token")
    class InvalidToken {

        @Test
        @DisplayName("Should return 401 when token verification fails")
        void shouldReturn401OnInvalidToken() throws ServletException, IOException, FirebaseAuthException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            request.addHeader("Authorization", "Bearer expired-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(mockFirebaseAuth.verifyIdToken("expired-token"))
                    .thenThrow(new FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "Token expired", null, null, null));

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
