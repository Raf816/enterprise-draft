package com.staffs.leavebooking.staffmanagement.application.handlers;

import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;
import com.staffs.leavebooking.staffmanagement.infrastructure.repositories.StaffMemberRepository;
import com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException;
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
 * Unit tests for the Staff Management CQRS Query Handler.
 * Mocks the repository to test delegation, mapping, and exception handling in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Staff Query Handler")
class StaffQueryHandlerTest {

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @InjectMocks
    private StaffQueryHandler queryHandler;

    @Nested
    @DisplayName("findAllStaffMembers")
    class FindAll {

        @Test
        @DisplayName("Should return all staff members as DTOs")
        void shouldReturnAllStaffMembers() {
            // Arrange
            when(staffMemberRepository.findAll())
                    .thenReturn(List.of(
                            createTestStaffJpa("staff-1", "James", "Wilson"),
                            createTestStaffJpa("staff-2", "Emily", "Chen")
                    ));

            // Act
            List<StaffMemberDTO> result = queryHandler.findAllStaffMembers();

            // Assert
            assertEquals(2, result.size());
            assertEquals("James", result.get(0).firstName());
            assertEquals("Emily", result.get(1).firstName());
        }

        @Test
        @DisplayName("Should return empty list when no staff exist")
        void shouldReturnEmptyWhenNoStaff() {
            // Arrange
            when(staffMemberRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<StaffMemberDTO> result = queryHandler.findAllStaffMembers();

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findStaffMemberById")
    class FindById {

        @Test
        @DisplayName("Should return a single staff member DTO")
        void shouldReturnStaffById() {
            // Arrange
            when(staffMemberRepository.findById("staff-1"))
                    .thenReturn(Optional.of(createTestStaffJpa("staff-1", "James", "Wilson")));

            // Act
            StaffMemberDTO result = queryHandler.findStaffMemberById("staff-1");

            // Assert
            assertEquals("staff-1", result.id());
            assertEquals("James", result.firstName());
            assertEquals("Wilson", result.surname());
            assertEquals("Engineering", result.department());
        }

        @Test
        @DisplayName("Should throw StaffMemberNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // Arrange
            when(staffMemberRepository.findById("unknown"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(StaffMemberNotFoundException.class,
                    () -> queryHandler.findStaffMemberById("unknown"));
        }
    }

    @Nested
    @DisplayName("findByDepartment")
    class FindByDepartment {

        @Test
        @DisplayName("Should filter staff by department")
        void shouldFilterByDepartment() {
            // Arrange
            when(staffMemberRepository.findByDepartment("Engineering"))
                    .thenReturn(List.of(createTestStaffJpa("staff-1", "James", "Wilson")));

            // Act
            List<StaffMemberDTO> result = queryHandler.findByDepartment("Engineering");

            // Assert
            assertEquals(1, result.size());
            assertEquals("Engineering", result.get(0).department());
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("Should filter staff by employment status")
        void shouldFilterByStatus() {
            // Arrange
            when(staffMemberRepository.findByEmploymentStatus("ACTIVE"))
                    .thenReturn(List.of(createTestStaffJpa("staff-1", "James", "Wilson")));

            // Act
            List<StaffMemberDTO> result = queryHandler.findByStatus("ACTIVE");

            // Assert
            assertEquals(1, result.size());
            verify(staffMemberRepository).findByEmploymentStatus("ACTIVE");
        }
    }

    @Nested
    @DisplayName("findByManagerId")
    class FindByManagerId {

        @Test
        @DisplayName("Should return staff managed by a specific manager")
        void shouldReturnStaffForManager() {
            // Arrange
            when(staffMemberRepository.findByLineManagerId("mgr-1"))
                    .thenReturn(List.of(
                            createTestStaffJpa("staff-1", "James", "Wilson"),
                            createTestStaffJpa("staff-2", "Emily", "Chen")
                    ));

            // Act
            List<StaffMemberDTO> result = queryHandler.findByManagerId("mgr-1");

            // Assert
            assertEquals(2, result.size());
            verify(staffMemberRepository).findByLineManagerId("mgr-1");
        }
    }

    // ---------------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------------

    private StaffMemberJpa createTestStaffJpa(String id, String firstName, String surname) {
        StaffMemberJpa jpa = new StaffMemberJpa();
        jpa.setId(id);
        jpa.setFirstName(firstName);
        jpa.setSurname(surname);
        jpa.setEmail(firstName.toLowerCase() + "." + surname.toLowerCase() + "@company.com");
        jpa.setDepartment("Engineering");
        jpa.setLineManagerId("mgr-1");
        jpa.setHireDate(LocalDate.of(2022, 6, 1));
        jpa.setCurrentRole("Software Engineer");
        jpa.setStartDateCurrentRole(LocalDate.of(2022, 6, 1));
        jpa.setJobLevel("L4");
        jpa.setEmploymentType("FULL_TIME");
        jpa.setEmploymentStatus("ACTIVE");
        return jpa;
    }
}
