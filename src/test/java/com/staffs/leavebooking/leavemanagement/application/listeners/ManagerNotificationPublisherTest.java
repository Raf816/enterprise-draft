package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.Event;
import com.staffs.leavebooking.common.events.ManagerNotificationEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerNotificationPublisher")
class ManagerNotificationPublisherTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private DomainEventManager domainEventManager;

    @InjectMocks
    private ManagerNotificationPublisher publisher;

    private static final String LEAVE_REQUEST_ID = "lr-001";
    private static final String STAFF_MEMBER_ID = "staff-001";
    private static final String MANAGER_ID = "mgr-001";
    private static final int NUMBER_OF_DAYS = 5;

    @Test
    @DisplayName("Should publish ManagerNotificationEvent via DomainEventManager when leave request is submitted")
    void shouldPublishManagerNotificationOnSubmit() {
        // Arrange
        LeaveRequestSubmittedEvent submittedEvent = new LeaveRequestSubmittedEvent(
                LocalDate.now(), LEAVE_REQUEST_ID, STAFF_MEMBER_ID, NUMBER_OF_DAYS);

        LeaveRequestJpa jpa = new LeaveRequestJpa();
        jpa.setId(LEAVE_REQUEST_ID);
        jpa.setManagerId(MANAGER_ID);
        jpa.setStaffMemberId(STAFF_MEMBER_ID);
        jpa.setStartDate(LocalDate.of(2026, 9, 1));
        jpa.setEndDate(LocalDate.of(2026, 9, 5));
        jpa.setReason("Holiday");

        when(leaveRequestRepository.findById(LEAVE_REQUEST_ID)).thenReturn(Optional.of(jpa));

        // Act
        publisher.onLeaveRequestSubmitted(submittedEvent);

        // Assert — verify DomainEventManager was called with the notification event
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainEventManager).manageDomainEvents(eq("ManagerNotificationPublisher"), captor.capture());

        List<Event> events = captor.getValue();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ManagerNotificationEvent.class);

        ManagerNotificationEvent notification = (ManagerNotificationEvent) events.get(0);
        assertThat(notification.managerId()).isEqualTo(MANAGER_ID);
        assertThat(notification.staffMemberId()).isEqualTo(STAFF_MEMBER_ID);
        assertThat(notification.leaveRequestId()).isEqualTo(LEAVE_REQUEST_ID);
        assertThat(notification.numberOfDays()).isEqualTo(NUMBER_OF_DAYS);
        assertThat(notification.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(notification.endDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(notification.reason()).isEqualTo("Holiday");
        assertThat(notification.occurredOn()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should not publish notification when leave request is not found")
    void shouldNotPublishWhenLeaveRequestNotFound() {
        // Arrange
        LeaveRequestSubmittedEvent submittedEvent = new LeaveRequestSubmittedEvent(
                LocalDate.now(), "non-existent-id", STAFF_MEMBER_ID, NUMBER_OF_DAYS);

        when(leaveRequestRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        // Act
        publisher.onLeaveRequestSubmitted(submittedEvent);

        // Assert
        verify(domainEventManager, never()).manageDomainEvents(any(), any());
    }
}
