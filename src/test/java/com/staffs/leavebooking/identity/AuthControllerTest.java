package com.staffs.leavebooking.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.staffs.leavebooking.identity.authService.FirebaseAuthService;
import com.staffs.leavebooking.identity.dto.LoginRequest;
import com.staffs.leavebooking.identity.dto.LoginResponse;
import com.staffs.leavebooking.identity.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AuthController using @WebMvcTest.
 * Tests register, login, and role-check endpoints.
 * FirebaseAuthService is fully mocked — no external Firebase calls.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("Auth Controller")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private com.staffs.leavebooking.staffmanagement.StaffManagementFacade staffManagementFacade;

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("Should return 201 Created on successful registration")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn201OnSuccess() throws Exception {
            // Arrange
            var request = new RegisterRequest("admin1", "admin@email.com", "password123", "ADMIN");
            UserRecord mockRecord = mock(UserRecord.class);
            when(mockRecord.getUid()).thenReturn("uid-123");
            when(mockRecord.getEmail()).thenReturn("admin@email.com");
            when(mockRecord.getDisplayName()).thenReturn("admin1");
            when(firebaseAuthService.registerUser(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(mockRecord);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid").value("uid-123"))
                    .andExpect(jsonPath("$.email").value("admin@email.com"))
                    .andExpect(jsonPath("$.message").value("User created successfully"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when Firebase rejects registration")
        @WithMockUser
        void shouldReturn400OnFirebaseError() throws Exception {
            // Arrange
            var request = new RegisterRequest("admin1", "existing@email.com", "password123", "ADMIN");
            when(firebaseAuthService.registerUser(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(mock(FirebaseAuthException.class));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("Should return 200 with login response on success")
        @WithMockUser
        void shouldReturn200OnSuccess() throws Exception {
            // Arrange
            var request = new LoginRequest("admin@email.com", "password123");
            var response = new LoginResponse("uid-123", "admin@email.com", "admin1",
                    "mock-jwt-token", "mock-refresh-token", "3600");
            when(firebaseAuthService.loginUser("admin@email.com", "password123"))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.localId").value("uid-123"))
                    .andExpect(jsonPath("$.idToken").value("mock-jwt-token"));
        }
    }

    @Nested
    @DisplayName("GET /auth/role-check")
    class RoleCheck {

        @Test
        @DisplayName("Should return roles for authenticated user")
        @WithMockUser(username = "uid-123", roles = "ADMIN")
        void shouldReturnRoles() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/auth/role-check"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("ROLE_ADMIN")));
        }
    }

    @Nested
    @DisplayName("PATCH /auth/password")
    class ChangePassword {

        @Test
        @DisplayName("Should return 200 on successful password change")
        @WithMockUser(username = "uid-staff-001")
        void shouldReturn200OnSuccess() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/auth/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"NewSecurePass123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("Should return 400 when newPassword is missing")
        @WithMockUser(username = "uid-staff-001")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(patch("/auth/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should require authentication — 401 without token")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(patch("/auth/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"Test123!\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
