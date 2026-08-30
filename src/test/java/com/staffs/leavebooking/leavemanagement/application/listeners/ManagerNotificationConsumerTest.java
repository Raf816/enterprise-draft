package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.ManagerNotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerNotificationConsumer (RabbitMQ Consumer)")
class ManagerNotificationConsumerTest {

    @InjectMocks
    private ManagerNotificationConsumer consumer;

    @Test
    @DisplayName("Should process manager notification event without error")
    void shouldProcessNotificationEvent() {
        // Arrange
        ManagerNotificationEvent event = new ManagerNotificationEvent(
                LocalDate.now(),
                "mgr-001",
                "staff-001",
                "John Smith",
                "lr-001",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5),
                5,
                "Holiday"
        );

        // Act & Assert — consumer logs only, should not throw
        assertThatNoException().isThrownBy(() -> consumer.receive(event));
    }

    @Test
    @DisplayName("Should handle event with null reason gracefully")
    void shouldHandleNullReason() {
        // Arrange
        ManagerNotificationEvent event = new ManagerNotificationEvent(
                LocalDate.now(),
                "mgr-002",
                "staff-002",
                "Jane Doe",
                "lr-002",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3),
                3,
                null
        );

        // Act & Assert
        assertThatNoException().isThrownBy(() -> consumer.receive(event));
    }
}
