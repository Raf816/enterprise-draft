package com.staffs.leavebooking.staffmanagement.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffs.leavebooking.staffmanagement.StaffManagementFacade;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
@ActiveProfiles("test")
@DisplayName("Staff Controller")
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StaffManagementFacade facade;

    @MockBean
    private com.staffs.leavebooking.identity.authService.FirebaseAuthService firebaseAuthService;

    // ═══════════════════════════════════════════════════════════════════
    // GET /staff (unfiltered)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /staff")
    class GetAllStaff {

        @Test
        @DisplayName("Should return all staff members (unfiltered)")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturnAllStaff() throws Exception {
            when(facade.findAllStaffMembers())
                    .thenReturn(List.of(createDTO("staff-1", "James", "Wilson", "ACTIVE")));

            mockMvc.perform(get("/staff"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("staff-1"));
        }
    }

    @Nested
    @DisplayName("GET /staff/{id}")
    class GetById {

        @Test
        @DisplayName("Should return a single staff member")
        @WithMockUser(username = "mgr-1", roles = "MANAGER")
        void shouldReturnStaffById() throws Exception {
            when(facade.findStaffMemberById("staff-1"))
                    .thenReturn(createDTO("staff-1", "James", "Wilson", "ACTIVE"));

            mockMvc.perform(get("/staff/staff-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.surname").value("Wilson"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /staff/search (filtered)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /staff/search")
    class SearchStaff {

        @Test
        @DisplayName("Should filter by department")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldFilterByDepartment() throws Exception {
            when(facade.searchStaff(any(StaffSearchCriteria.class)))
                    .thenReturn(List.of(createDTO("staff-1", "James", "Wilson", "ACTIVE")));

            mockMvc.perform(post("/staff/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"department\": \"Engineering\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("staff-1"));
        }

        @Test
        @DisplayName("Should filter by status")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldFilterByStatus() throws Exception {
            when(facade.searchStaff(any(StaffSearchCriteria.class)))
                    .thenReturn(List.of(createDTO("staff-1", "Raf", "Ahmed", "PENDING_SETUP")));

            mockMvc.perform(post("/staff/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"PENDING_SETUP\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].employmentStatus").value("PENDING_SETUP"));
        }

        @Test
        @DisplayName("Should filter by department AND status combined")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldFilterByBoth() throws Exception {
            when(facade.searchStaff(any(StaffSearchCriteria.class)))
                    .thenReturn(List.of(createDTO("staff-1", "James", "Wilson", "ACTIVE")));

            mockMvc.perform(post("/staff/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"department\": \"Networks\", \"status\": \"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("staff-1"));
        }

        @Test
        @DisplayName("Should return 400 when no filters provided")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturn400WhenNoFilters() throws Exception {
            mockMvc.perform(post("/staff/search")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /staff (create)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /staff")
    class AddStaffMember {

        @Test
        @DisplayName("Should create Firebase user and staff record, return 201")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldReturn201() throws Exception {
            String body = """
                    {
                        "firstName": "James",
                        "surname": "Wilson",
                        "email": "james@company.com",
                        "department": "Engineering",
                        "lineManagerId": "mgr-1",
                        "hireDate": "2022-06-01",
                        "currentRole": "Software Engineer",
                        "startDateOfCurrentRole": "2022-06-01",
                        "jobLevel": "L4",
                        "employmentType": "FULL_TIME"
                    }
                    """;

            var mockUserRecord = org.mockito.Mockito.mock(com.google.firebase.auth.UserRecord.class);
            when(mockUserRecord.getUid()).thenReturn("firebase-uid-123");
            when(firebaseAuthService.registerUser(any(), any(), any(), any())).thenReturn(mockUserRecord);
            when(facade.addStaffMemberWithId(eq("firebase-uid-123"), any())).thenReturn("firebase-uid-123");

            mockMvc.perform(post("/staff")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("james@company.com"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PATCH /staff/{id} (unified update)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /staff/{id}")
    class UpdateStaff {

        @Test
        @DisplayName("Should update department only")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldUpdateDepartmentOnly() throws Exception {
            when(facade.findStaffMemberById("staff-1"))
                    .thenReturn(createDTO("staff-1", "James", "Wilson", "ACTIVE"));

            mockMvc.perform(patch("/staff/staff-1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"department\": \"Marketing\", \"lineManagerId\": \"mgr-2\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should update status only")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldUpdateStatusOnly() throws Exception {
            when(facade.findStaffMemberById("staff-1"))
                    .thenReturn(createDTO("staff-1", "James", "Wilson", "ACTIVE"));

            mockMvc.perform(patch("/staff/staff-1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"employmentStatus\": \"ACTIVE\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should update Firebase role when role field present")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldUpdateFirebaseRole() throws Exception {
            when(facade.findStaffMemberById("staff-1"))
                    .thenReturn(createDTO("staff-1", "James", "Wilson", "ACTIVE"));

            mockMvc.perform(patch("/staff/staff-1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\": \"MANAGER\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should update all fields in a single request")
        @WithMockUser(username = "admin-1", roles = "ADMIN")
        void shouldUpdateAllFieldsAtOnce() throws Exception {
            when(facade.findStaffMemberById("staff-1"))
                    .thenReturn(createDTO("staff-1", "James", "Wilson", "ACTIVE"));

            String body = """
                    {
                        "department": "Digital",
                        "lineManagerId": "mgr-2",
                        "currentRole": "Senior Dev",
                        "startDateOfCurrentRole": "2026-09-01",
                        "jobLevel": "SENIOR",
                        "employmentType": "FULL_TIME",
                        "employmentStatus": "ACTIVE",
                        "role": "MANAGER"
                    }
                    """;

            mockMvc.perform(patch("/staff/staff-1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private StaffMemberDTO createDTO(String id, String firstName, String surname, String status) {
        return new StaffMemberDTO(
                id, firstName, surname, firstName.toLowerCase() + "@company.com",
                "Engineering", "mgr-1", LocalDate.of(2022, 6, 1),
                "Software Engineer", LocalDate.of(2022, 6, 1),
                "L4", "FULL_TIME", status
        );
    }
}
