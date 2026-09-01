package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.Event;
import com.staffs.leavebooking.common.events.StaffNotificationEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffNotificationPublisher")
class StaffNotificationPublisherTest {

    @Mock
    private DomainEventManager domainEventManager;

    @InjectMocks
    private StaffNotificationPublisher publisher;

    private static final String LEAVE_REQUEST_ID = "lr-001";
    private static final String STAFF_MEMBER_ID = "staff-001";
    private static final String MANAGER_ID = "mgr-001";
    private static final int NUMBER_OF_DAYS = 3;

    @Nested
    @DisplayName("onLeaveRequestApproved")
    class OnApproved {

        @Test
        @DisplayName("Should publish StaffNotificationEvent with APPROVED decision via DomainEventManager")
        void shouldPublishApprovedNotification() {
            // Arrange
            LeaveRequestApprovedEvent approvedEvent = new LeaveRequestApprovedEvent(
                    LocalDate.now(), LEAVE_REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, NUMBER_OF_DAYS);

            // Act
            publisher.onLeaveRequestApproved(approvedEvent);

            // Assert
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
            verify(domainEventManager).manageDomainEvents(eq("StaffNotificationPublisher"), captor.capture());

            StaffNotificationEvent notification = (StaffNotificationEvent) captor.getValue().get(0);
            assertThat(notification.staffMemberId()).isEqualTo(STAFF_MEMBER_ID);
            assertThat(notification.leaveRequestId()).isEqualTo(LEAVE_REQUEST_ID);
            assertThat(notification.decision()).isEqualTo("APPROVED");
            assertThat(notification.decidedBy()).isEqualTo(MANAGER_ID);
            assertThat(notification.numberOfDays()).isEqualTo(NUMBER_OF_DAYS);
            assertThat(notification.occurredOn()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("onLeaveRequestRejected")
    class OnRejected {

        @Test
        @DisplayName("Should publish StaffNotificationEvent with REJECTED decision via DomainEventManager")
        void shouldPublishRejectedNotification() {
            // Arrange
            LeaveRequestRejectedEvent rejectedEvent = new LeaveRequestRejectedEvent(
                    LocalDate.now(), LEAVE_REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, NUMBER_OF_DAYS);

            // Act
            publisher.onLeaveRequestRejected(rejectedEvent);

            // Assert
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
            verify(domainEventManager).manageDomainEvents(eq("StaffNotificationPublisher"), captor.capture());

            StaffNotificationEvent notification = (StaffNotificationEvent) captor.getValue().get(0);
            assertThat(notification.staffMemberId()).isEqualTo(STAFF_MEMBER_ID);
            assertThat(notification.leaveRequestId()).isEqualTo(LEAVE_REQUEST_ID);
            assertThat(notification.decision()).isEqualTo("REJECTED");
            assertThat(notification.decidedBy()).isEqualTo(MANAGER_ID);
            assertThat(notification.numberOfDays()).isEqualTo(NUMBER_OF_DAYS);
        }
    }

    @Nested
    @DisplayName("onLeaveRequestCancelled")
    class OnCancelled {

        @Test
        @DisplayName("Should publish StaffNotificationEvent with CANCELLED decision via DomainEventManager")
        void shouldPublishCancelledNotification() {
            // Arrange
            LeaveRequestCancelledEvent cancelledEvent = new LeaveRequestCancelledEvent(
                    LocalDate.now(), LEAVE_REQUEST_ID, STAFF_MEMBER_ID, "staff-001",
                    NUMBER_OF_DAYS, false);

            // Act
            publisher.onLeaveRequestCancelled(cancelledEvent);

            // Assert
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
            verify(domainEventManager).manageDomainEvents(eq("StaffNotificationPublisher"), captor.capture());

            StaffNotificationEvent notification = (StaffNotificationEvent) captor.getValue().get(0);
            assertThat(notification.staffMemberId()).isEqualTo(STAFF_MEMBER_ID);
            assertThat(notification.leaveRequestId()).isEqualTo(LEAVE_REQUEST_ID);
            assertThat(notification.decision()).isEqualTo("CANCELLED");
            assertThat(notification.decidedBy()).isEqualTo("staff-001");
            assertThat(notification.numberOfDays()).isEqualTo(NUMBER_OF_DAYS);
        }
    }
}
