package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestSubmittedListener")
class LeaveRequestSubmittedListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private LeaveRequestSubmittedListener listener;

    @Test
    @DisplayName("Should reserve days on allowance when leave request submitted")
    void shouldReserveDays() {
        // Arrange
        var event = new LeaveRequestSubmittedEvent(LocalDate.now(), "req-1", "staff-1", 5);

        // Act
        listener.handle(event);

        // Assert
        verify(leaveAllowanceApplicationService).reserveDays("staff-1", 5);
    }
}
