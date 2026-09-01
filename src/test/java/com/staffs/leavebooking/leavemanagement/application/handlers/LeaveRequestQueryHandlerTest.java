package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestSearchCriteria;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LeaveRequest CQRS Query Handler.
 * Tests basic queries (GET endpoints) and search queries (POST /search endpoints).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequest Query Handler")
class LeaveRequestQueryHandlerTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveRequestQueryHandler queryHandler;

    // ═══════════════════════════════════════════════════════════════════
    // BASIC QUERIES (GET endpoints)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findRequestsByStaffMemberId")
    class FindByStaffMemberId {

        @Test
        @DisplayName("Should return mapped DTOs for a staff member")
        void shouldReturnRequestsForStaff() {
            // Arrange
            when(leaveRequestRepository.findByStaffMemberId("staff-1"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            // Act
            List<LeaveRequestDTO> result = queryHandler.findRequestsByStaffMemberId("staff-1");

            // Assert
            assertEquals(1, result.size());
            assertEquals("req-1", result.get(0).id());
            verify(leaveRequestRepository).findByStaffMemberId("staff-1");
        }

        @Test
        @DisplayName("Should return empty list when no requests found")
        void shouldReturnEmptyListWhenNone() {
            // Arrange
            when(leaveRequestRepository.findByStaffMemberId("unknown"))
                    .thenReturn(Collections.emptyList());

            // Act
            List<LeaveRequestDTO> result = queryHandler.findRequestsByStaffMemberId("unknown");

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findRequestsByManagerId")
    class FindByManagerId {

        @Test
        @DisplayName("Should return all requests for a manager's team")
        void shouldReturnRequestsForManager() {
            // Arrange
            when(leaveRequestRepository.findByManagerId("mgr-1"))
                    .thenReturn(List.of(
                            createJpa("req-1", "staff-1", "mgr-1"),
                            createJpa("req-2", "staff-2", "mgr-1")));

            // Act
            List<LeaveRequestDTO> result = queryHandler.findRequestsByManagerId("mgr-1");

            // Assert
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("findAllRequests")
    class FindAll {

        @Test
        @DisplayName("Should return all requests company-wide")
        void shouldReturnAllRequests() {
            // Arrange
            when(leaveRequestRepository.findAll())
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            // Act
            List<LeaveRequestDTO> result = queryHandler.findAllRequests();

            // Assert
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("findRequestById")
    class FindById {

        @Test
        @DisplayName("Should return a single request by ID")
        void shouldReturnRequestById() {
            // Arrange
            when(leaveRequestRepository.findById("req-1"))
                    .thenReturn(Optional.of(createJpa("req-1", "staff-1", "mgr-1")));

            // Act
            LeaveRequestDTO result = queryHandler.findRequestById("req-1");

            // Assert
            assertEquals("req-1", result.id());
        }

        @Test
        @DisplayName("Should throw LeaveRequestNotFoundException when ID not found")
        void shouldThrowWhenNotFound() {
            // Arrange
            when(leaveRequestRepository.findById("unknown"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(LeaveRequestNotFoundException.class,
                    () -> queryHandler.findRequestById("unknown"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SEARCH QUERIES (POST /search endpoints)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchByStaffMember (POST /my/search)")
    class SearchByStaffMember {

        @Test
        @DisplayName("Should filter by status only")
        void shouldFilterByStatus() {
            // Arrange
            LeaveRequestJpa jpa = createJpa("req-1", "staff-1", "mgr-1");
            jpa.setStatus("APPROVED");
            when(leaveRequestRepository.findByStaffMemberIdAndStatus("staff-1", "APPROVED"))
                    .thenReturn(List.of(jpa));

            var criteria = new LeaveRequestSearchCriteria("approved", null, null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchByStaffMember("staff-1", criteria);

            // Assert
            assertEquals(1, result.size());
            assertEquals("APPROVED", result.get(0).status());
        }

        @Test
        @DisplayName("Should return all when no filters provided")
        void shouldReturnAllWhenNoFilters() {
            // Arrange
            when(leaveRequestRepository.findByStaffMemberId("staff-1"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, null, null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchByStaffMember("staff-1", criteria);

            // Assert
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("searchByManager (POST /team/search)")
    class SearchByManager {

        @Test
        @DisplayName("Should filter by status only")
        void shouldFilterByStatus() {
            // Arrange
            when(leaveRequestRepository.findByManagerIdAndStatus("mgr-1", "PENDING"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria("pending", null, null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchByManager("mgr-1", criteria);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter by date range only")
        void shouldFilterByDateRange() {
            // Arrange
            LocalDate from = LocalDate.of(2026, 9, 1);
            LocalDate to = LocalDate.of(2026, 9, 30);
            when(leaveRequestRepository.findByManagerIdAndDateOverlap("mgr-1", from, to))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, null, null, from, to);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchByManager("mgr-1", criteria);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter by status AND date range combined")
        void shouldFilterByStatusAndDateRange() {
            // Arrange
            LocalDate from = LocalDate.of(2026, 9, 1);
            LocalDate to = LocalDate.of(2026, 12, 31);
            when(leaveRequestRepository.findByManagerIdAndStatusAndDateOverlap("mgr-1", "PENDING", from, to))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria("pending", null, null, from, to);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchByManager("mgr-1", criteria);

            // Assert
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("searchAll (POST /all/search)")
    class SearchAll {

        @Test
        @DisplayName("Should filter by staffMemberId")
        void shouldFilterByStaffMemberId() {
            // Arrange
            when(leaveRequestRepository.findByStaffMemberId("staff-1"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, "staff-1", null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
            assertEquals("staff-1", result.get(0).staffMemberId());
        }

        @Test
        @DisplayName("Should filter by managerId")
        void shouldFilterByManagerId() {
            // Arrange
            when(leaveRequestRepository.findByManagerId("mgr-1"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, null, "mgr-1", null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter by status company-wide")
        void shouldFilterByStatus() {
            // Arrange
            LeaveRequestJpa jpa = createJpa("req-1", "staff-1", "mgr-1");
            jpa.setStatus("REJECTED");
            when(leaveRequestRepository.findByStatus("REJECTED"))
                    .thenReturn(List.of(jpa));

            var criteria = new LeaveRequestSearchCriteria("rejected", null, null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
            assertEquals("REJECTED", result.get(0).status());
        }

        @Test
        @DisplayName("Should filter by managerId + status combined")
        void shouldFilterByManagerAndStatus() {
            // Arrange
            when(leaveRequestRepository.findByManagerIdAndStatus("mgr-1", "PENDING"))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria("pending", null, "mgr-1", null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter by date range company-wide")
        void shouldFilterByDateRange() {
            // Arrange
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            when(leaveRequestRepository.findByDateOverlap(from, to))
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, null, null, from, to);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return all when no filters provided")
        void shouldReturnAllWhenNoFilters() {
            // Arrange
            when(leaveRequestRepository.findAll())
                    .thenReturn(List.of(createJpa("req-1", "staff-1", "mgr-1")));

            var criteria = new LeaveRequestSearchCriteria(null, null, null, null, null);

            // Act
            List<LeaveRequestDTO> result = queryHandler.searchAll(criteria);

            // Assert
            assertEquals(1, result.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private LeaveRequestJpa createJpa(String id, String staffId, String managerId) {
        LeaveRequestJpa jpa = new LeaveRequestJpa();
        jpa.setId(id);
        jpa.setStaffMemberId(staffId);
        jpa.setManagerId(managerId);
        jpa.setLeaveType("ANNUAL");
        jpa.setStartDate(LocalDate.now().plusDays(7));
        jpa.setEndDate(LocalDate.now().plusDays(11));
        jpa.setNumberOfDays(5);
        jpa.setReason("Test reason");
        jpa.setStatus("PENDING");
        jpa.setSubmittedOn(LocalDate.now());
        return jpa;
    }
}
