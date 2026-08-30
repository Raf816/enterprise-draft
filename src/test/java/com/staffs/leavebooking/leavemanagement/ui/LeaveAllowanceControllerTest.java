package com.staffs.leavebooking.leavemanagement.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffs.leavebooking.leavemanagement.LeaveManagementFacade;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for LeaveAllowanceController using @WebMvcTest.
 * Tests HTTP mapping, status codes, and JSON response structure.
 */
@WebMvcTest(LeaveAllowanceController.class)
@ActiveProfiles("test")
@DisplayName("LeaveAllowance Controller")
class LeaveAllowanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveManagementFacade facade;

    @Nested
    @DisplayName("GET /leave-allowances/my")
    class GetMyAllowance {

        @Test
        @DisplayName("Should return current user's allowance")
        @WithMockUser(username = "staff-1")
        void shouldReturnMyAllowance() throws Exception {
            // Arrange
            when(facade.findMyAllowance("staff-1")).thenReturn(createTestAllowanceDTO("allow-1", "staff-1"));

            // Act & Assert
            mockMvc.perform(get("/leave-allowances/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("allow-1"))
                    .andExpect(jsonPath("$.totalEntitlement").value(25))
                    .andExpect(jsonPath("$.remainingDays").value(20));
        }
    }

    @Nested
    @DisplayName("GET /leave-allowances/staff/{staffMemberId}")
    class GetForStaff {

        @Test
        @DisplayName("Should return allowance for a specific staff member")
        @WithMockUser(username = "mgr-1", roles = "MANAGER")
        void shouldReturnAllowanceForStaff() throws Exception {
            // Arrange
            when(facade.findAllowanceForStaffMember("staff-1"))
                    .thenReturn(createTestAllowanceDTO("allow-1", "staff-1"));

            // Act & Assert
            mockMvc.perform(get("/leave-allowances/staff/staff-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.staffMemberId").value("staff-1"));
        }
    }

    @Nested
    @DisplayName("GET /leave-allowances/team")
    class GetTeamAllowances {

        @Test
        @DisplayName("Should return team allowances for manager")
        @WithMockUser(username = "mgr-1", roles = "MANAGER")
        void shouldReturnTeamAllowances() throws Exception {
            // Arrange
            when(facade.findTeamAllowances("mgr-1"))
                    .thenReturn(List.of(createTestAllowanceDTO("allow-1", "staff-1")));

            // Act & Assert
            mockMvc.perform(get("/leave-allowances/team"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("allow-1"));
        }
    }

    @Nested
    @DisplayName("GET /leave-allowances/all")
    class GetAll {

        @Test
        @DisplayName("Should return all allowances for admin")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturnAllAllowances() throws Exception {
            // Arrange
            when(facade.findAllAllowances())
                    .thenReturn(List.of(createTestAllowanceDTO("allow-1", "staff-1")));

            // Act & Assert
            mockMvc.perform(get("/leave-allowances/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should filter by department when provided")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldFilterByDepartment() throws Exception {
            // Arrange
            when(facade.findAllowancesByDepartment("Engineering"))
                    .thenReturn(List.of(createTestAllowanceDTO("allow-1", "staff-1")));

            // Act & Assert
            mockMvc.perform(get("/leave-allowances/all").param("department", "Engineering"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].department").value("Engineering"));
        }
    }

    @Nested
    @DisplayName("PATCH /leave-allowances/{id}")
    class AmendEntitlement {

        @Test
        @DisplayName("Should amend entitlement and return updated allowance")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldAmendEntitlement() throws Exception {
            // Arrange
            var updated = new LeaveAllowanceDTO(
                    "allow-1", "staff-1", "James Wilson", "mgr-1",
                    "Engineering", "2026-2027", 30, 5, 3, 25, 22);
            when(facade.findAllowanceById("allow-1")).thenReturn(updated);

            // Act & Assert
            mockMvc.perform(patch("/leave-allowances/allow-1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newEntitlement\": 30}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEntitlement").value(30));

            verify(facade).amendEntitlement(any());
        }
    }

    // ---------------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------------

    private LeaveAllowanceDTO createTestAllowanceDTO(String id, String staffId) {
        return new LeaveAllowanceDTO(
                id, staffId, "James Wilson", "mgr-1", "Engineering",
                "2026-2027", 25, 5, 3, 20, 17
        );
    }
}
