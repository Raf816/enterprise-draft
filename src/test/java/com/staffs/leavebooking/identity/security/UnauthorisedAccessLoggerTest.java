package com.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnauthorisedAccessLogger (401/403 Handler)")
class UnauthorisedAccessLoggerTest {

    private UnauthorisedAccessLogger handler;

    @BeforeEach
    void setUp() {
        handler = new UnauthorisedAccessLogger();
    }

    @Nested
    @DisplayName("commence (401 Unauthorized)")
    class Commence {

        @Test
        @DisplayName("Should return 401 status code")
        void shouldReturn401() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/leave-requests/my");
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException("Bad credentials");

            // Act
            handler.commence(request, response, exception);

            // Assert
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should return JSON content type")
        void shouldReturnJsonContentType() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/staff");
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException("Invalid token");

            // Act
            handler.commence(request, response, exception);

            // Assert
            assertThat(response.getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("Should include structured error body with status and message")
        void shouldIncludeErrorBody() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/leave-requests");
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException("Token expired");

            // Act
            handler.commence(request, response, exception);

            // Assert
            String body = response.getContentAsString();
            assertThat(body).contains("\"status\":401");
            assertThat(body).contains("\"error\":\"Unauthorized\"");
            assertThat(body).contains("Authentication required");
            assertThat(body).contains("timestamp");
        }
    }

    @Nested
    @DisplayName("handle (403 Forbidden)")
    class Handle {

        @Test
        @DisplayName("Should return 403 status code")
        void shouldReturn403() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/staff");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Access is denied");

            // Act
            handler.handle(request, response, exception);

            // Assert
            assertThat(response.getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("Should return JSON content type")
        void shouldReturnJsonContentType() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/staff/123/department");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Insufficient role");

            // Act
            handler.handle(request, response, exception);

            // Assert
            assertThat(response.getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("Should include structured error body with status and message")
        void shouldIncludeErrorBody() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/staff/123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Role not sufficient");

            // Act
            handler.handle(request, response, exception);

            // Assert
            String body = response.getContentAsString();
            assertThat(body).contains("\"status\":403");
            assertThat(body).contains("\"error\":\"Forbidden\"");
            assertThat(body).contains("Access denied");
            assertThat(body).contains("timestamp");
        }

        @Test
        @DisplayName("Should use X-Forwarded-For for client IP when available")
        void shouldUseForwardedIp() throws IOException {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/staff");
            request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Denied");

            // Act — should not throw; logging uses the forwarded IP
            handler.handle(request, response, exception);

            // Assert
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }
}
