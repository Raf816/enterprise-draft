package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestApprovedListener")
class LeaveRequestApprovedListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private LeaveRequestApprovedListener listener;

    @Test
    @DisplayName("Should confirm days on allowance when leave request approved")
    void shouldConfirmDays() {
        // Arrange
        var event = new LeaveRequestApprovedEvent(LocalDate.now(), "req-1", "staff-1", "mgr-1", 5);

        // Act
        listener.handle(event);

        // Assert
        verify(leaveAllowanceApplicationService).confirmDays("staff-1", 5);
    }
}
