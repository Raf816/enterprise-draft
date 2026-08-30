package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestCancelledListener")
class LeaveRequestCancelledListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private LeaveRequestCancelledListener listener;

    @Nested
    @DisplayName("When previously approved")
    class WhenPreviouslyApproved {

        @Test
        @DisplayName("Should credit back days (daysUsed -= days)")
        void shouldCreditBackDays() {
            // Arrange
            var event = new LeaveRequestCancelledEvent(
                    LocalDate.now(), "req-1", "staff-1", "staff-1", 5, true);

            // Act
            listener.handle(event);

            // Assert
            verify(leaveAllowanceApplicationService).creditBackDays("staff-1", 5);
            verifyNoMoreInteractions(leaveAllowanceApplicationService);
        }
    }

    @Nested
    @DisplayName("When still pending (not previously approved)")
    class WhenStillPending {

        @Test
        @DisplayName("Should release pending days (daysPending -= days)")
        void shouldReleasePendingDays() {
            // Arrange
            var event = new LeaveRequestCancelledEvent(
                    LocalDate.now(), "req-1", "staff-1", "staff-1", 5, false);

            // Act
            listener.handle(event);

            // Assert
            verify(leaveAllowanceApplicationService).releasePendingDays("staff-1", 5);
            verifyNoMoreInteractions(leaveAllowanceApplicationService);
        }
    }
}
