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
import static org.mockito.Mockito.*;

@DisplayName("RateLimitFilter (Brute-Force Protection)")
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("Non-login endpoints")
    class NonLoginEndpoints {

        @Test
        @DisplayName("Should pass through for non-login endpoints without rate limiting")
        void shouldPassThroughForNonLoginEndpoints() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should pass through for GET requests to login endpoint")
        void shouldPassThroughForGetLogin() throws ServletException, IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Login rate limiting")
    class LoginRateLimiting {

        @Test
        @DisplayName("Should allow first 20 POST login requests")
        void shouldAllowFirst20Requests() throws ServletException, IOException {
            // Act & Assert — 20 requests should all pass
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
                request.setRemoteAddr("192.168.1.1");
                MockHttpServletResponse response = new MockHttpServletResponse();

                filter.doFilterInternal(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(200);
            }

            verify(filterChain, times(20)).doFilter(any(), any());
        }

        @Test
        @DisplayName("Should return 429 on 21st POST login request from same IP")
        void shouldReturn429On21stRequest() throws ServletException, IOException {
            // Arrange — exhaust the bucket with 20 requests
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
                request.setRemoteAddr("10.0.0.1");
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilterInternal(request, response, filterChain);
            }

            // Act — 21st request
            MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/auth/login");
            blockedRequest.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            filter.doFilterInternal(blockedRequest, blockedResponse, filterChain);

            // Assert
            assertThat(blockedResponse.getStatus()).isEqualTo(429);
            assertThat(blockedResponse.getContentAsString()).contains("Rate limit exceeded");
            assertThat(blockedResponse.getContentAsString()).contains("Too Many Requests");
            assertThat(blockedResponse.getContentType()).isEqualTo("application/json");

            // Filter chain should NOT be called for the 21st request
            verify(filterChain, times(20)).doFilter(any(), any());
        }

        @Test
        @DisplayName("Should rate limit per IP — different IPs have separate buckets")
        void shouldRateLimitPerIp() throws ServletException, IOException {
            // Arrange — exhaust bucket for IP 1
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
                request.setRemoteAddr("192.168.1.100");
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilterInternal(request, response, filterChain);
            }

            // Act — request from a DIFFERENT IP should still pass
            MockHttpServletRequest differentIpRequest = new MockHttpServletRequest("POST", "/auth/login");
            differentIpRequest.setRemoteAddr("192.168.1.200");
            MockHttpServletResponse differentIpResponse = new MockHttpServletResponse();
            filter.doFilterInternal(differentIpRequest, differentIpResponse, filterChain);

            // Assert
            assertThat(differentIpResponse.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should use X-Forwarded-For header for client IP when present")
        void shouldUseXForwardedForHeader() throws ServletException, IOException {
            // Arrange — exhaust bucket for forwarded IP
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
                request.setRemoteAddr("proxy-server");
                request.addHeader("X-Forwarded-For", "real-client-ip, proxy-1");
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilterInternal(request, response, filterChain);
            }

            // Act — 21st request with same forwarded IP
            MockHttpServletRequest sixthRequest = new MockHttpServletRequest("POST", "/auth/login");
            sixthRequest.setRemoteAddr("proxy-server");
            sixthRequest.addHeader("X-Forwarded-For", "real-client-ip, proxy-1");
            MockHttpServletResponse sixthResponse = new MockHttpServletResponse();
            filter.doFilterInternal(sixthRequest, sixthResponse, filterChain);

            // Assert — should be rate limited
            assertThat(sixthResponse.getStatus()).isEqualTo(429);
        }
    }
}
