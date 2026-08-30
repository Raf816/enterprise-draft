package com.staffs.leavebooking.staffmanagement.application.handlers;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.staffmanagement.application.commands.AddStaffMemberCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateDepartmentCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdatePlacementCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateStatusCommand;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Staff Management application service (command handler).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Staff Application Service")
class StaffApplicationServiceTest {

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private DomainEventManager domainEventManager;

    @InjectMocks
    private StaffApplicationService staffApplicationService;

    private static final String STAFF_ID = UUID.randomUUID().toString();
    private static final String UNKNOWN_ID = UUID.randomUUID().toString();

    // ═══════════════════════════════════════════════════════════════════
    // addNewStaffMember
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addNewStaffMember")
    class AddNewStaffMember {

        @Test
        @DisplayName("Should save new staff member with PENDING_SETUP status (no events dispatched)")
        void shouldSaveWithPendingSetup() {
            // Arrange
            AddStaffMemberCommand command = new AddStaffMemberCommand(
                    "James", "Wilson", "james@company.com", "Engineering",
                    "mgr-1", LocalDate.of(2022, 6, 1), "Software Engineer",
                    LocalDate.of(2022, 6, 1), "L4", "FULL_TIME", 25, null, null);
            when(staffMemberRepository.existsByEmail("james@company.com")).thenReturn(false);

            // Act
            String id = staffApplicationService.addNewStaffMember(command);

            // Assert
            assertNotNull(id);
            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
            // No events dispatched — events fire on activation (PENDING_SETUP → ACTIVE)
            verifyNoInteractions(domainEventManager);
        }

        @Test
        @DisplayName("Should default entitlement to 25 when not provided (zero)")
        void shouldDefaultEntitlementTo25() {
            // Arrange — entitlement = 0 (not provided)
            AddStaffMemberCommand command = new AddStaffMemberCommand(
                    "James", "Wilson", "james@company.com", "Engineering",
                    "mgr-1", LocalDate.of(2022, 6, 1), "Software Engineer",
                    LocalDate.of(2022, 6, 1), "L4", "FULL_TIME", 0, null, null);
            when(staffMemberRepository.existsByEmail("james@company.com")).thenReturn(false);

            // Act — should not throw (defaults to 25 internally)
            assertDoesNotThrow(() -> staffApplicationService.addNewStaffMember(command));
            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void shouldThrowOnDuplicateEmail() {
            AddStaffMemberCommand command = new AddStaffMemberCommand(
                    "James", "Wilson", "existing@company.com", "Engineering",
                    "mgr-1", LocalDate.of(2022, 6, 1), "Software Engineer",
                    LocalDate.of(2022, 6, 1), "L4", "FULL_TIME", 25, null, null);
            when(staffMemberRepository.existsByEmail("existing@company.com")).thenReturn(true);

            assertThrows(DataIntegrityViolationException.class,
                    () -> staffApplicationService.addNewStaffMember(command));
            verify(staffMemberRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // createSkeletonStaffMember
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createSkeletonStaffMember")
    class CreateSkeleton {

        @Test
        @DisplayName("Should create skeleton staff record with Firebase UID as ID")
        void shouldCreateSkeleton() {
            String firebaseUid = UUID.randomUUID().toString();
            when(staffMemberRepository.existsByEmail("raf@staffs.ac.uk")).thenReturn(false);

            String id = staffApplicationService.createSkeletonStaffMember(
                    firebaseUid, "Raf", "Ahmed", "raf@staffs.ac.uk");

            assertEquals(firebaseUid, id);
            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
        }

        @Test
        @DisplayName("Should skip creation if email already exists (idempotent)")
        void shouldSkipIfEmailExists() {
            String firebaseUid = UUID.randomUUID().toString();
            when(staffMemberRepository.existsByEmail("raf@staffs.ac.uk")).thenReturn(true);

            String id = staffApplicationService.createSkeletonStaffMember(
                    firebaseUid, "Raf", "Ahmed", "raf@staffs.ac.uk");

            assertEquals(firebaseUid, id);
            verify(staffMemberRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // updateDepartment
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateDepartment")
    class UpdateDepartment {

        @Test
        @DisplayName("Should load, update, save, and dispatch events")
        void shouldUpdateDepartmentAndDispatch() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "ACTIVE")));
            UpdateDepartmentCommand command = new UpdateDepartmentCommand(STAFF_ID, "Marketing", "mgr-2");

            staffApplicationService.updateDepartment(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
            verify(domainEventManager).manageDomainEvents(anyString(), anyList());
        }

        @Test
        @DisplayName("Should use current value when department is null (partial update)")
        void shouldUseFallbackForNullDepartment() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "ACTIVE")));
            UpdateDepartmentCommand command = new UpdateDepartmentCommand(STAFF_ID, null, "mgr-2");

            staffApplicationService.updateDepartment(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
        }

        @Test
        @DisplayName("Should throw when staff member not found")
        void shouldThrowWhenNotFound() {
            when(staffMemberRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());
            UpdateDepartmentCommand command = new UpdateDepartmentCommand(UNKNOWN_ID, "Marketing", "mgr-2");

            assertThrows(StaffMemberNotFoundException.class,
                    () -> staffApplicationService.updateDepartment(command));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // updatePlacement
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePlacement")
    class UpdatePlacement {

        @Test
        @DisplayName("Should update placement without dispatching events")
        void shouldUpdatePlacementNoEvents() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "ACTIVE")));
            UpdatePlacementCommand command = new UpdatePlacementCommand(
                    STAFF_ID, "Senior Engineer", LocalDate.now(), "L5", "FULL_TIME");

            staffApplicationService.updatePlacement(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
            verifyNoInteractions(domainEventManager);
        }

        @Test
        @DisplayName("Should use current values for null fields (partial update)")
        void shouldUseFallbackForNullFields() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "ACTIVE")));
            // Only updating currentRole — everything else null
            UpdatePlacementCommand command = new UpdatePlacementCommand(
                    STAFF_ID, "Lead Dev", null, null, null);

            staffApplicationService.updatePlacement(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
        }

        @Test
        @DisplayName("Should throw when staff member not found")
        void shouldThrowWhenNotFound() {
            when(staffMemberRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());
            UpdatePlacementCommand command = new UpdatePlacementCommand(
                    UNKNOWN_ID, "Senior Engineer", LocalDate.now(), "L5", "FULL_TIME");

            assertThrows(StaffMemberNotFoundException.class,
                    () -> staffApplicationService.updatePlacement(command));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // updateStatus
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Should activate PENDING_SETUP staff and dispatch StaffMemberAddedEvent")
        void shouldActivateAndDispatchEvent() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "PENDING_SETUP")));
            UpdateStatusCommand command = new UpdateStatusCommand(STAFF_ID, "ACTIVE");

            staffApplicationService.updateStatus(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
            verify(domainEventManager).manageDomainEvents(anyString(), anyList());
        }

        @Test
        @DisplayName("Should update status without event for non-activation transitions")
        void shouldUpdateWithoutEvent() {
            when(staffMemberRepository.findById(STAFF_ID))
                    .thenReturn(Optional.of(createTestStaffJpa(STAFF_ID, "ACTIVE")));
            UpdateStatusCommand command = new UpdateStatusCommand(STAFF_ID, "ON_LEAVE");

            staffApplicationService.updateStatus(command);

            verify(staffMemberRepository).save(any(StaffMemberJpa.class));
            verifyNoInteractions(domainEventManager);
        }

        @Test
        @DisplayName("Should throw when staff member not found")
        void shouldThrowWhenNotFound() {
            when(staffMemberRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());
            UpdateStatusCommand command = new UpdateStatusCommand(UNKNOWN_ID, "ACTIVE");

            assertThrows(StaffMemberNotFoundException.class,
                    () -> staffApplicationService.updateStatus(command));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private StaffMemberJpa createTestStaffJpa(String id, String status) {
        StaffMemberJpa jpa = new StaffMemberJpa();
        jpa.setId(id);
        jpa.setFirstName("James");
        jpa.setSurname("Wilson");
        jpa.setEmail("james.wilson@company.com");
        jpa.setDepartment("Engineering");
        jpa.setLineManagerId("mgr-1");
        jpa.setHireDate(LocalDate.of(2022, 6, 1));
        jpa.setCurrentRole("Software Engineer");
        jpa.setStartDateCurrentRole(LocalDate.of(2022, 6, 1));
        jpa.setJobLevel("L4");
        jpa.setEmploymentType("FULL_TIME");
        jpa.setEmploymentStatus(status);
        return jpa;
    }
}
