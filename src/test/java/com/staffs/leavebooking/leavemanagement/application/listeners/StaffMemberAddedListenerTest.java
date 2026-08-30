package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffMemberAddedEvent;
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
@DisplayName("StaffMemberAddedListener (Remote Event)")
class StaffMemberAddedListenerTest {

    @Mock
    private LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    @InjectMocks
    private StaffMemberAddedListener listener;

    @Test
    @DisplayName("Should create allowance for new staff member from remote event")
    void shouldCreateAllowance() {
        // Arrange
        var event = new StaffMemberAddedEvent(
                LocalDate.now(), "staff-1", "James", "Wilson",
                "james.wilson@company.com", "mgr-1", "Engineering", 25);

        // Act
        listener.receive(event);

        // Assert
        verify(leaveAllowanceApplicationService).createAllowanceForNewStaff(
                "staff-1", "mgr-1", "James", "Wilson", "Engineering", 25);
    }
}
