package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestRejectedListener")
class LeaveRequestRejectedListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private LeaveRequestRejectedListener listener;

    @Test
    @DisplayName("Should release pending days when leave request rejected")
    void shouldReleasePendingDays() {
        // Arrange
        var event = new LeaveRequestRejectedEvent(LocalDate.now(), "req-1", "staff-1", "mgr-1", 5);

        // Act
        listener.handle(event);

        // Assert
        verify(leaveAllowanceApplicationService).releasePendingDays("staff-1", 5);
    }
}
