package com.staffs.leavebooking.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("SecurityHeadersFilter (Server Obfuscation & Hardening)")
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        filterChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("Standard security headers")
    class StandardHeaders {

        @Test
        @DisplayName("Should obfuscate server header (empty string)")
        void shouldObfuscateServerHeader() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("Server")).isEmpty();
        }

        @Test
        @DisplayName("Should remove X-Powered-By header")
        void shouldRemoveXPoweredBy() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/staff");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("X-Powered-By")).isEmpty();
        }

        @Test
        @DisplayName("Should set X-Content-Type-Options to nosniff")
        void shouldSetNoSniff() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }

        @Test
        @DisplayName("Should set Strict-Transport-Security header")
        void shouldSetHsts() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("Strict-Transport-Security"))
                    .isEqualTo("max-age=31536000; includeSubDomains");
        }

        @Test
        @DisplayName("Should set Cache-Control to no-store")
        void shouldSetCacheControl() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        }

        @Test
        @DisplayName("Should set X-XSS-Protection to 0 (CSP preferred)")
        void shouldSetXssProtection() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("0");
        }

        @Test
        @DisplayName("Should continue the filter chain after setting headers")
        void shouldContinueFilterChain() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/staff");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("X-Frame-Options (clickjacking protection)")
    class FrameOptions {

        @Test
        @DisplayName("Should set X-Frame-Options to DENY for normal endpoints")
        void shouldSetDenyForNormalEndpoints() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        }

        @Test
        @DisplayName("Should set X-Frame-Options to SAMEORIGIN for H2 console")
        void shouldSetSameoriginForH2Console() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/h2-console/login.do");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        }
    }
}
