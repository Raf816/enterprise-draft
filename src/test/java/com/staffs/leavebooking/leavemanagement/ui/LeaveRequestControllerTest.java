package com.staffs.leavebooking.leavemanagement.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffs.leavebooking.leavemanagement.LeaveManagementFacade;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestSearchCriteria;
import com.staffs.leavebooking.staffmanagement.StaffManagementFacade;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for LeaveRequestController using @WebMvcTest.
 * Tests HTTP mapping, status codes, and JSON response structure.
 * Uses @WithMockUser for authentication context.
 */
@WebMvcTest(LeaveRequestController.class)
@ActiveProfiles("test")
@DisplayName("LeaveRequest Controller")
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveManagementFacade facade;

    @MockBean
    private StaffManagementFacade staffFacade;

    // ═══════════════════════════════════════════════════════════════════
    // GET ENDPOINTS (simple, unfiltered reads)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /leave-requests/{id}")
    class GetById {

        @Test
        @DisplayName("Should return 200 with leave request DTO")
        @WithMockUser(username = "staff-1")
        void shouldReturnRequest() throws Exception {
            // Arrange
            var dto = createTestDTO("req-1", "staff-1", "PENDING");
            when(facade.findRequestById("req-1")).thenReturn(dto);

            // Act & Assert
            mockMvc.perform(get("/leave-requests/req-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("req-1"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("GET /leave-requests/my")
    class GetMyRequests {

        @Test
        @DisplayName("Should return user's own requests (unfiltered)")
        @WithMockUser(username = "staff-1")
        void shouldReturnMyRequests() throws Exception {
            // Arrange
            when(facade.findMyRequests("staff-1"))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            // Act & Assert
            mockMvc.perform(get("/leave-requests/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("req-1"));
        }
    }

    @Nested
    @DisplayName("GET /leave-requests/team")
    class GetTeamRequests {

        @Test
        @DisplayName("Should return manager's team requests (unfiltered)")
        @WithMockUser(username = "mgr-1")
        void shouldReturnTeamRequests() throws Exception {
            // Arrange
            when(facade.findTeamRequests("mgr-1"))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            // Act & Assert
            mockMvc.perform(get("/leave-requests/team"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("req-1"));
        }
    }

    @Nested
    @DisplayName("GET /leave-requests/all")
    class GetAllRequests {

        @Test
        @DisplayName("Should return all requests for admin (unfiltered)")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturnAllRequests() throws Exception {
            // Arrange
            when(facade.findAllRequests())
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            // Act & Assert
            mockMvc.perform(get("/leave-requests/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST SEARCH ENDPOINTS (filtered queries via JSON body)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /leave-requests/my/search")
    class SearchMyRequests {

        @Test
        @DisplayName("Should search own requests by status")
        @WithMockUser(username = "staff-1")
        void shouldSearchByStatus() throws Exception {
            // Arrange
            when(facade.searchMyRequests(eq("staff-1"), any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            var criteria = new LeaveRequestSearchCriteria("PENDING", null, null, null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/my/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            verify(facade).searchMyRequests(eq("staff-1"), any(LeaveRequestSearchCriteria.class));
        }

        @Test
        @DisplayName("Should return 400 when no filters provided")
        @WithMockUser(username = "staff-1")
        void shouldReturn400WhenNoFilters() throws Exception {
            var criteria = new LeaveRequestSearchCriteria(null, null, null, null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/my/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());

            verify(facade, never()).searchMyRequests(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when staffMemberId filter is provided")
        @WithMockUser(username = "staff-1")
        void shouldRejectStaffMemberIdFilter() throws Exception {
            var criteria = new LeaveRequestSearchCriteria("PENDING", "other-staff", null, null, null);

            mockMvc.perform(post("/leave-requests/my/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when managerId filter is provided")
        @WithMockUser(username = "staff-1")
        void shouldRejectManagerIdFilter() throws Exception {
            var criteria = new LeaveRequestSearchCriteria("PENDING", null, "mgr-1", null, null);

            mockMvc.perform(post("/leave-requests/my/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /leave-requests/team/search")
    class SearchTeamRequests {

        @Test
        @DisplayName("Should search team requests by status")
        @WithMockUser(username = "mgr-1")
        void shouldSearchByStatus() throws Exception {
            // Arrange
            when(facade.searchTeamRequests(eq("mgr-1"), any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            var criteria = new LeaveRequestSearchCriteria("PENDING", null, null, null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/team/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Should search team requests by date range")
        @WithMockUser(username = "mgr-1")
        void shouldSearchByDateRange() throws Exception {
            // Arrange
            when(facade.searchTeamRequests(eq("mgr-1"), any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "APPROVED")));

            var criteria = new LeaveRequestSearchCriteria(
                    null, null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

            // Act & Assert
            mockMvc.perform(post("/leave-requests/team/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("req-1"));
        }

        @Test
        @DisplayName("Should search team requests by status AND date range")
        @WithMockUser(username = "mgr-1")
        void shouldSearchByStatusAndDateRange() throws Exception {
            // Arrange
            when(facade.searchTeamRequests(eq("mgr-1"), any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            var criteria = new LeaveRequestSearchCriteria(
                    "PENDING", null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));

            // Act & Assert
            mockMvc.perform(post("/leave-requests/team/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 400 when staffMemberId filter is provided")
        @WithMockUser(username = "mgr-1")
        void shouldRejectStaffMemberIdFilter() throws Exception {
            var criteria = new LeaveRequestSearchCriteria("PENDING", "staff-1", null, null, null);

            mockMvc.perform(post("/leave-requests/team/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when managerId filter is provided")
        @WithMockUser(username = "mgr-1")
        void shouldRejectManagerIdFilter() throws Exception {
            var criteria = new LeaveRequestSearchCriteria("PENDING", null, "mgr-2", null, null);

            mockMvc.perform(post("/leave-requests/team/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /leave-requests/all/search")
    class SearchAllRequests {

        @Test
        @DisplayName("Should search by staffMemberId")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldSearchByStaffMemberId() throws Exception {
            // Arrange
            when(facade.searchAllRequests(any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            var criteria = new LeaveRequestSearchCriteria(null, "staff-1", null, null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].staffMemberId").value("staff-1"));
        }

        @Test
        @DisplayName("Should search by managerId")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldSearchByManagerId() throws Exception {
            // Arrange
            when(facade.searchAllRequests(any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(
                            createTestDTO("req-1", "staff-1", "PENDING"),
                            createTestDTO("req-2", "staff-2", "APPROVED")));

            var criteria = new LeaveRequestSearchCriteria(null, null, "mgr-1", null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should search by managerId + status combined")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldSearchByManagerAndStatus() throws Exception {
            // Arrange
            when(facade.searchAllRequests(any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "PENDING")));

            var criteria = new LeaveRequestSearchCriteria("PENDING", null, "mgr-1", null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Should search by date range company-wide")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldSearchByDateRange() throws Exception {
            // Arrange
            when(facade.searchAllRequests(any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of(createTestDTO("req-1", "staff-1", "APPROVED")));

            var criteria = new LeaveRequestSearchCriteria(
                    null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

            // Act & Assert
            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("req-1"));
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturnEmptyList() throws Exception {
            // Arrange
            when(facade.searchAllRequests(any(LeaveRequestSearchCriteria.class)))
                    .thenReturn(List.of());

            var criteria = new LeaveRequestSearchCriteria(null, "unknown-staff", null, null, null);

            // Act & Assert
            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 when both staffMemberId and managerId are provided")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldRejectCombinedStaffAndManagerFilters() throws Exception {
            var criteria = new LeaveRequestSearchCriteria(null, "staff-1", "mgr-1", null, null);

            mockMvc.perform(post("/leave-requests/all/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(criteria)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMMAND ENDPOINTS (POST/PATCH — write operations)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /leave-requests")
    class SubmitRequest {

        @Test
        @DisplayName("Should return 201 Created on valid submission (ACTIVE staff)")
        @WithMockUser(username = "staff-1")
        void shouldReturn201OnSubmit() throws Exception {
            // Arrange — staff is ACTIVE with lineManagerId "mgr-1"
            when(staffFacade.findStaffMemberByIdInternal("staff-1"))
                    .thenReturn(createTestStaffDTO("staff-1", "ACTIVE"));
            // Manager exists and is ACTIVE
            when(staffFacade.findStaffMemberByIdInternal("mgr-1"))
                    .thenReturn(createTestStaffDTO("mgr-1", "ACTIVE"));
            // No overlapping requests
            when(facade.findMyRequests("staff-1")).thenReturn(java.util.List.of());
            // Sufficient allowance (25 available)
            when(facade.findMyAllowanceInternal("staff-1")).thenReturn(
                    new com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO(
                            "allow-1", "staff-1", "Test User", "mgr-1", "Engineering",
                            "2026-2027", 25, 0, 0, 25, 25));

            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(11),
                    "ANNUAL",
                    "Holiday"
            );
            when(facade.submitLeaveRequest(any())).thenReturn("new-req-id");
            when(facade.findRequestById("new-req-id")).thenReturn(createTestDTO("new-req-id", "staff-1", "PENDING"));

            // Act & Assert
            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("new-req-id"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 403 when staff is PENDING_SETUP")
        @WithMockUser(username = "pending-user")
        void shouldBlock403WhenPendingSetup() throws Exception {
            // Arrange — staff is PENDING_SETUP
            when(staffFacade.findStaffMemberByIdInternal("pending-user"))
                    .thenReturn(createTestStaffDTO("pending-user", "PENDING_SETUP"));
            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(11),
                    "ANNUAL",
                    "Holiday"
            );

            // Act & Assert
            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());

            verify(facade, never()).submitLeaveRequest(any());
        }

        @Test
        @DisplayName("Should return 403 when staff is TERMINATED")
        @WithMockUser(username = "terminated-user")
        void shouldBlock403WhenTerminated() throws Exception {
            // Arrange — staff is TERMINATED
            when(staffFacade.findStaffMemberByIdInternal("terminated-user"))
                    .thenReturn(createTestStaffDTO("terminated-user", "TERMINATED"));
            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(11),
                    "ANNUAL",
                    "Holiday"
            );

            // Act & Assert
            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden());

            verify(facade, never()).submitLeaveRequest(any());
        }

        @Test
        @DisplayName("Should return 400 when staff has no line manager assigned")
        @WithMockUser(username = "no-mgr-user")
        void shouldReturn400WhenNoLineManager() throws Exception {
            // Arrange — staff is ACTIVE but has no lineManagerId
            when(staffFacade.findStaffMemberByIdInternal("no-mgr-user"))
                    .thenReturn(new com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO(
                            "no-mgr-user", "Test", "User", "test@test.com",
                            "Engineering", null, LocalDate.of(2022, 6, 1),
                            "Developer", LocalDate.of(2022, 6, 1),
                            "L4", "FULL_TIME", "ACTIVE"));
            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(11),
                    "ANNUAL",
                    "Holiday"
            );

            // Act & Assert
            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(facade, never()).submitLeaveRequest(any());
        }

        @Test
        @DisplayName("Should return 400 when assigned manager no longer exists")
        @WithMockUser(username = "orphan-user")
        void shouldReturn400WhenManagerNotFound() throws Exception {
            // Arrange — staff has lineManagerId "deleted-mgr" but that person no longer exists
            when(staffFacade.findStaffMemberByIdInternal("orphan-user"))
                    .thenReturn(new com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO(
                            "orphan-user", "Test", "User", "test@test.com",
                            "Engineering", "deleted-mgr", LocalDate.of(2022, 6, 1),
                            "Developer", LocalDate.of(2022, 6, 1),
                            "L4", "FULL_TIME", "ACTIVE"));
            when(staffFacade.findStaffMemberByIdInternal("deleted-mgr"))
                    .thenThrow(new RuntimeException("Not found"));
            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7), LocalDate.now().plusDays(11), "ANNUAL", "Holiday");

            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
            verify(facade, never()).submitLeaveRequest(any());
        }

        @Test
        @DisplayName("Should return 400 when assigned manager is TERMINATED")
        @WithMockUser(username = "term-mgr-user")
        void shouldReturn400WhenManagerTerminated() throws Exception {
            // Arrange — staff has lineManagerId "term-mgr" who is TERMINATED
            when(staffFacade.findStaffMemberByIdInternal("term-mgr-user"))
                    .thenReturn(new com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO(
                            "term-mgr-user", "Test", "User", "test@test.com",
                            "Engineering", "term-mgr", LocalDate.of(2022, 6, 1),
                            "Developer", LocalDate.of(2022, 6, 1),
                            "L4", "FULL_TIME", "ACTIVE"));
            when(staffFacade.findStaffMemberByIdInternal("term-mgr"))
                    .thenReturn(new com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO(
                            "term-mgr", "Old", "Manager", "old@test.com",
                            "Engineering", null, LocalDate.of(2020, 1, 1),
                            "Manager", LocalDate.of(2020, 1, 1),
                            "L6", "FULL_TIME", "TERMINATED"));
            var body = new SubmitLeaveRequestBody(
                    LocalDate.now().plusDays(7), LocalDate.now().plusDays(11), "ANNUAL", "Holiday");

            mockMvc.perform(post("/leave-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
            verify(facade, never()).submitLeaveRequest(any());
        }
    }

    @Nested
    @DisplayName("PATCH /leave-requests/{id}/approve")
    class ApproveRequest {

        @Test
        @DisplayName("Should approve when authenticated user is the assigned manager")
        @WithMockUser(username = "mgr-1")
        void shouldApproveAsAssignedManager() throws Exception {
            // Arrange — managerId matches authenticated user
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            // Act & Assert
            mockMvc.perform(patch("/leave-requests/req-1/approve")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Approved, enjoy your holiday\"}"))
                    .andExpect(status().isOk());

            verify(facade).approveLeaveRequest(eq("req-1"), eq("mgr-1"), any());
        }

        @Test
        @DisplayName("Should return 403 when manager is not the assigned manager")
        @WithMockUser(username = "other-mgr", roles = "MANAGER")
        void shouldReject403WhenNotAssignedManager() throws Exception {
            // Arrange — managerId is "mgr-1", not "other-mgr"
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            // Act & Assert
            mockMvc.perform(patch("/leave-requests/req-1/approve").with(csrf()))
                    .andExpect(status().isForbidden());

            verify(facade, never()).approveLeaveRequest(any(), any(), any());
        }

        @Test
        @DisplayName("Should approve as admin even if not the assigned manager")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldApproveAsAdmin() throws Exception {
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/approve").with(csrf()))
                    .andExpect(status().isOk());

            verify(facade).approveLeaveRequest(eq("req-1"), eq("admin-1"), any());
        }
    }

    @Nested
    @DisplayName("PATCH /leave-requests/{id}/reject")
    class RejectRequest {

        @Test
        @DisplayName("Should reject with optional reason")
        @WithMockUser(username = "mgr-1")
        void shouldRejectWithReason() throws Exception {
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/reject")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Team is short-staffed that week\"}"))
                    .andExpect(status().isOk());

            verify(facade).rejectLeaveRequest(eq("req-1"), eq("mgr-1"), any());
        }

        @Test
        @DisplayName("Should return 403 when not the assigned manager")
        @WithMockUser(username = "other-mgr", roles = "MANAGER")
        void shouldReject403WhenNotAssigned() throws Exception {
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/reject").with(csrf()))
                    .andExpect(status().isForbidden());

            verify(facade, never()).rejectLeaveRequest(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /leave-requests/{id}/cancel")
    class CancelRequest {

        @Test
        @DisplayName("Should cancel own request with reason")
        @WithMockUser(username = "staff-1")
        void shouldCancelOwnRequest() throws Exception {
            // Arrange — staffMemberId matches authenticated user
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/cancel")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Changed plans\"}"))
                    .andExpect(status().isOk());

            verify(facade).cancelLeaveRequest(any());
        }

        @Test
        @DisplayName("Should return 403 when cancelling someone else's request")
        @WithMockUser(username = "other-staff")
        void shouldReject403WhenNotOwner() throws Exception {
            // Arrange — staffMemberId is "staff-1", not "other-staff"
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/cancel")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Trying to cancel someone else's\"}"))
                    .andExpect(status().isForbidden());

            verify(facade, never()).cancelLeaveRequest(any());
        }

        @Test
        @DisplayName("Should cancel as admin even if not the owner")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldCancelAsAdmin() throws Exception {
            when(facade.findRequestById("req-1"))
                    .thenReturn(createTestDTO("req-1", "staff-1", "PENDING"));

            mockMvc.perform(patch("/leave-requests/req-1/cancel")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Admin cancellation\"}"))
                    .andExpect(status().isOk());

            verify(facade).cancelLeaveRequest(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private LeaveRequestDTO createTestDTO(String id, String staffId, String status) {
        return new LeaveRequestDTO(
                id, staffId, "mgr-1", "ANNUAL",
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(11),
                5, "Test reason", status, LocalDate.now(),
                null, null, null, null
        );
    }

    private StaffMemberDTO createTestStaffDTO(String id, String employmentStatus) {
        return new StaffMemberDTO(
                id, "Test", "User", "test@test.com",
                "Engineering", "mgr-1", LocalDate.of(2022, 6, 1),
                "Developer", LocalDate.of(2022, 6, 1),
                "L4", "FULL_TIME", employmentStatus
        );
    }
}
