package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveAllowanceRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LeaveAllowance CQRS Query Handler.
 * Mocks the repository to test delegation, mapping, and exception handling in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveAllowance Query Handler")
class LeaveAllowanceQueryHandlerTest {

    @Mock
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @InjectMocks
    private LeaveAllowanceQueryHandler queryHandler;

    @Nested
    @DisplayName("findAllowanceByStaffMemberId")
    class FindByStaffMemberId {

        @Test
        @DisplayName("Should return the current allowance for a staff member")
        void shouldReturnAllowanceForStaff() {
            // Arrange
            when(leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc("staff-1"))
                    .thenReturn(Optional.of(createTestAllowanceJpa("allow-1", "staff-1")));

            // Act
            LeaveAllowanceDTO result = queryHandler.findAllowanceByStaffMemberId("staff-1");

            // Assert
            assertEquals("allow-1", result.id());
            assertEquals("staff-1", result.staffMemberId());
            assertEquals("James Wilson", result.staffName());
            assertEquals(25, result.totalEntitlement());
            assertEquals(20, result.remainingDays()); // 25 - 5
            assertEquals(17, result.availableDays()); // 25 - 5 - 3
        }

        @Test
        @DisplayName("Should throw LeaveAllowanceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // Arrange
            when(leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc("unknown"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(LeaveAllowanceNotFoundException.class,
                    () -> queryHandler.findAllowanceByStaffMemberId("unknown"));
        }
    }

    @Nested
    @DisplayName("findAllowanceById")
    class FindById {

        @Test
        @DisplayName("Should return allowance by its ID")
        void shouldReturnById() {
            // Arrange
            when(leaveAllowanceRepository.findById("allow-1"))
                    .thenReturn(Optional.of(createTestAllowanceJpa("allow-1", "staff-1")));

            // Act
            LeaveAllowanceDTO result = queryHandler.findAllowanceById("allow-1");

            // Assert
            assertEquals("allow-1", result.id());
        }

        @Test
        @DisplayName("Should throw LeaveAllowanceNotFoundException when ID not found")
        void shouldThrowWhenIdNotFound() {
            // Arrange
            when(leaveAllowanceRepository.findById("unknown"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(LeaveAllowanceNotFoundException.class,
                    () -> queryHandler.findAllowanceById("unknown"));
        }
    }

    @Nested
    @DisplayName("findAllowancesByManagerId")
    class FindByManagerId {

        @Test
        @DisplayName("Should return all allowances for a manager's team")
        void shouldReturnAllowancesForManager() {
            // Arrange
            when(leaveAllowanceRepository.findByManagerId("mgr-1"))
                    .thenReturn(List.of(
                            createTestAllowanceJpa("allow-1", "staff-1"),
                            createTestAllowanceJpa("allow-2", "staff-2")
                    ));

            // Act
            List<LeaveAllowanceDTO> result = queryHandler.findAllowancesByManagerId("mgr-1");

            // Assert
            assertEquals(2, result.size());
            verify(leaveAllowanceRepository).findByManagerId("mgr-1");
        }

        @Test
        @DisplayName("Should return empty list when manager has no team")
        void shouldReturnEmptyWhenNoTeam() {
            // Arrange
            when(leaveAllowanceRepository.findByManagerId("no-team"))
                    .thenReturn(Collections.emptyList());

            // Act
            List<LeaveAllowanceDTO> result = queryHandler.findAllowancesByManagerId("no-team");

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findAllAllowances")
    class FindAll {

        @Test
        @DisplayName("Should return all allowances company-wide")
        void shouldReturnAllAllowances() {
            // Arrange
            when(leaveAllowanceRepository.findAll())
                    .thenReturn(List.of(createTestAllowanceJpa("allow-1", "staff-1")));

            // Act
            List<LeaveAllowanceDTO> result = queryHandler.findAllAllowances();

            // Assert
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("findAllowancesByDepartment")
    class FindByDepartment {

        @Test
        @DisplayName("Should filter allowances by department")
        void shouldFilterByDepartment() {
            // Arrange
            when(leaveAllowanceRepository.findByDepartment("Engineering"))
                    .thenReturn(List.of(createTestAllowanceJpa("allow-1", "staff-1")));

            // Act
            List<LeaveAllowanceDTO> result = queryHandler.findAllowancesByDepartment("Engineering");

            // Assert
            assertEquals(1, result.size());
            assertEquals("Engineering", result.get(0).department());
        }
    }

    // ---------------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------------

    private LeaveAllowanceJpa createTestAllowanceJpa(String id, String staffId) {
        LeaveAllowanceJpa jpa = new LeaveAllowanceJpa();
        jpa.setId(id);
        jpa.setStaffMemberId(staffId);
        jpa.setManagerId("mgr-1");
        jpa.setFirstName("James");
        jpa.setSurname("Wilson");
        jpa.setDepartment("Engineering");
        jpa.setBusinessYearStart(2026);
        jpa.setBusinessYearEnd(2027);
        jpa.setTotalEntitlement(25);
        jpa.setDaysUsed(5);
        jpa.setDaysPending(3);
        return jpa;
    }
}
