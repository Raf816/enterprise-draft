package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffMemberUpdatedListener (Remote Event)")
class StaffMemberUpdatedListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private StaffMemberUpdatedListener listener;

    @Test
    @DisplayName("Should update staff details on allowance from remote event")
    void shouldUpdateStaffDetails() {
        // Arrange
        var event = new StaffMemberUpdatedEvent(LocalDate.now(), "staff-1", "mgr-2", "Marketing");

        // Act
        listener.receive(event);

        // Assert
        verify(leaveAllowanceApplicationService).updateStaffDetails("staff-1", "mgr-2", "Marketing");
    }
}
