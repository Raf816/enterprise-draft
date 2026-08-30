package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.leavemanagement.application.commands.CancelLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestApplicationService (CQRS Command Handler)")
class LeaveRequestApplicationServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private DomainEventManager domainEventManager;

    @InjectMocks
    private LeaveRequestApplicationService service;

    // ─────────────────────────────────────────────────────────────────
    // SUBMIT NEW REQUEST
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitNewRequest")
    class SubmitNewRequest {

        @Test
        @DisplayName("Should save leave request and dispatch events")
        void shouldSaveAndDispatchEvents() {
            // Arrange — future dates to pass validation
            LocalDate startDate = LocalDate.now().plusDays(5);
            LocalDate endDate = LocalDate.now().plusDays(9);

            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_UUID, MANAGER_UUID, startDate, endDate, "ANNUAL", "Holiday");

            // Act
            String id = service.submitNewRequest(command);

            // Assert
            assertThat(id).isNotNull().isNotBlank();

            // Verify JPA entity was saved
            ArgumentCaptor<LeaveRequestJpa> jpaCaptor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
            verify(leaveRequestRepository).save(jpaCaptor.capture());

            LeaveRequestJpa savedJpa = jpaCaptor.getValue();
            assertThat(savedJpa.getStaffMemberId()).isEqualTo(STAFF_UUID);
            assertThat(savedJpa.getManagerId()).isEqualTo(MANAGER_UUID);
            assertThat(savedJpa.getLeaveType()).isEqualTo("ANNUAL");
            assertThat(savedJpa.getStatus()).isEqualTo("PENDING");
            assertThat(savedJpa.getStartDate()).isEqualTo(startDate);
            assertThat(savedJpa.getEndDate()).isEqualTo(endDate);
            assertThat(savedJpa.getReason()).isEqualTo("Holiday");

            // Verify domain events were dispatched
            verify(domainEventManager).manageDomainEvents(
                    eq("LeaveRequestApplicationService"), any(List.class));
        }

        @Test
        @DisplayName("Should generate a valid UUID as the leave request ID")
        void shouldGenerateValidUuid() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(3);
            LocalDate endDate = LocalDate.now().plusDays(5);
            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_UUID, MANAGER_UUID, startDate, endDate, "ANNUAL", null);

            // Act
            String id = service.submitNewRequest(command);

            // Assert — should be a valid UUID format (36 chars with hyphens)
            assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid leave type")
        void shouldThrowForInvalidLeaveType() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(3);
            LocalDate endDate = LocalDate.now().plusDays(5);
            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_UUID, MANAGER_UUID, startDate, endDate, "SICK", null);

            // Act & Assert
            assertThatThrownBy(() -> service.submitNewRequest(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid leave type")
                    .hasMessageContaining("SICK");

            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for past start date")
        void shouldThrowForPastStartDate() {
            // Arrange
            LocalDate startDate = LocalDate.now().minusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(5);
            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_UUID, MANAGER_UUID, startDate, endDate, "ANNUAL", null);

            // Act & Assert
            assertThatThrownBy(() -> service.submitNewRequest(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(leaveRequestRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // APPROVE REQUEST
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveRequest")
    class ApproveRequest {

        @Test
        @DisplayName("Should approve a PENDING request and dispatch events")
        void shouldApprovePendingRequest() {
            // Arrange
            LeaveRequestJpa jpa = createPendingRequestJpa();
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            // Act
            service.approveRequest(LEAVE_REQUEST_UUID, MANAGER_UUID, null);

            // Assert
            ArgumentCaptor<LeaveRequestJpa> jpaCaptor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
            verify(leaveRequestRepository).save(jpaCaptor.capture());
            assertThat(jpaCaptor.getValue().getStatus()).isEqualTo("APPROVED");

            verify(domainEventManager).manageDomainEvents(
                    eq("LeaveRequestApplicationService"), any(List.class));
        }

        @Test
        @DisplayName("Should throw LeaveRequestNotFoundException when ID does not exist")
        void shouldThrowWhenNotFound() {
            // Arrange
            when(leaveRequestRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.approveRequest("non-existent", "mgr-001", null))
                    .isInstanceOf(LeaveRequestNotFoundException.class)
                    .hasMessageContaining("non-existent");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when approving non-PENDING request")
        void shouldThrowWhenNotPending() {
            // Arrange — already approved
            LeaveRequestJpa jpa = createPendingRequestJpa();
            jpa.setStatus("APPROVED");
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            // Act & Assert
            assertThatThrownBy(() -> service.approveRequest(LEAVE_REQUEST_UUID, MANAGER_UUID, null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // REJECT REQUEST
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        @Test
        @DisplayName("Should reject a PENDING request and dispatch events")
        void shouldRejectPendingRequest() {
            // Arrange
            LeaveRequestJpa jpa = createPendingRequestJpa();
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            // Act
            service.rejectRequest(LEAVE_REQUEST_UUID, MANAGER_UUID, null);

            // Assert
            ArgumentCaptor<LeaveRequestJpa> jpaCaptor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
            verify(leaveRequestRepository).save(jpaCaptor.capture());
            assertThat(jpaCaptor.getValue().getStatus()).isEqualTo("REJECTED");

            verify(domainEventManager).manageDomainEvents(
                    eq("LeaveRequestApplicationService"), any(List.class));
        }

        @Test
        @DisplayName("Should throw LeaveRequestNotFoundException when ID does not exist")
        void shouldThrowWhenNotFound() {
            // Arrange
            when(leaveRequestRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.rejectRequest("non-existent", "mgr-001", null))
                    .isInstanceOf(LeaveRequestNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL REQUEST
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("Should cancel a PENDING request and dispatch events")
        void shouldCancelPendingRequest() {
            // Arrange
            LeaveRequestJpa jpa = createPendingRequestJpa();
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            CancelLeaveRequestCommand command = new CancelLeaveRequestCommand(
                    LEAVE_REQUEST_UUID, STAFF_UUID, "Changed plans");

            // Act
            service.cancelRequest(command);

            // Assert
            ArgumentCaptor<LeaveRequestJpa> jpaCaptor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
            verify(leaveRequestRepository).save(jpaCaptor.capture());
            assertThat(jpaCaptor.getValue().getStatus()).isEqualTo("CANCELLED");

            verify(domainEventManager).manageDomainEvents(
                    eq("LeaveRequestApplicationService"), any(List.class));
        }

        @Test
        @DisplayName("Should cancel an APPROVED request")
        void shouldCancelApprovedRequest() {
            // Arrange
            LeaveRequestJpa jpa = createPendingRequestJpa();
            jpa.setStatus("APPROVED");
            jpa.setDecidedOn(LocalDate.now());
            jpa.setDecidedBy(MANAGER_UUID);
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            CancelLeaveRequestCommand command = new CancelLeaveRequestCommand(
                    LEAVE_REQUEST_UUID, STAFF_UUID, "Emergency");

            // Act
            service.cancelRequest(command);

            // Assert
            ArgumentCaptor<LeaveRequestJpa> jpaCaptor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
            verify(leaveRequestRepository).save(jpaCaptor.capture());
            assertThat(jpaCaptor.getValue().getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when cancelling REJECTED request")
        void shouldThrowWhenCancellingRejected() {
            // Arrange
            LeaveRequestJpa jpa = createPendingRequestJpa();
            jpa.setStatus("REJECTED");
            when(leaveRequestRepository.findById(LEAVE_REQUEST_UUID)).thenReturn(Optional.of(jpa));

            CancelLeaveRequestCommand command = new CancelLeaveRequestCommand(
                    LEAVE_REQUEST_UUID, STAFF_UUID, "Want to cancel");

            // Act & Assert
            assertThatThrownBy(() -> service.cancelRequest(command))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TEST FIXTURES
    // ─────────────────────────────────────────────────────────────────

    private static final String LEAVE_REQUEST_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String STAFF_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String MANAGER_UUID = "22222222-2222-2222-2222-222222222222";

    private LeaveRequestJpa createPendingRequestJpa() {
        LeaveRequestJpa jpa = new LeaveRequestJpa();
        jpa.setId(LEAVE_REQUEST_UUID);
        jpa.setStaffMemberId(STAFF_UUID);
        jpa.setManagerId(MANAGER_UUID);
        jpa.setLeaveType("ANNUAL");
        jpa.setStartDate(LocalDate.now().plusDays(5));
        jpa.setEndDate(LocalDate.now().plusDays(9));
        jpa.setNumberOfDays(5);
        jpa.setReason("Holiday");
        jpa.setStatus("PENDING");
        jpa.setSubmittedOn(LocalDate.now());
        return jpa;
    }
}
