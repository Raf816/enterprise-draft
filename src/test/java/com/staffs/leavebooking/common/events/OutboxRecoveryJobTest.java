package com.staffs.leavebooking.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OutboxRecoveryJob} — the scheduled poller that
 * re-publishes stranded PENDING and FAILED events to RabbitMQ.
 *
 * <p>Uses Mockito to mock EventStoreService, RabbitTemplate, RabbitOutboxRouter,
 * and ObjectMapper. Verifies correct delegation since the job is a void scheduled
 * method with no return value to assert on (justified verify() usage —
 * see docs/07 section 2.4).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRecoveryJob (Scheduled Poller)")
class OutboxRecoveryJobTest {

    @Mock private EventStoreService eventStoreService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RabbitOutboxRouter rabbitOutboxRouter;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxRecoveryJob recoveryJob;

    // ─── Helper ──────────────────────────────────────────────────────

    private EventStoreJpa createStrandedEvent(Long id, String eventType, String status, int retryCount) {
        var event = new EventStoreJpa();
        event.setId(id);
        event.setEventType(eventType);
        event.setEventBody("{\"id\":1,\"occurredOn\":\"2026-09-01\",\"staffMemberId\":\"staff-1\"}");
        event.setStatus(status);
        event.setRetryCount(retryCount);
        event.setOccurredOn(LocalDate.now().minusDays(1));
        event.setSourceContext("TestContext");
        return event;
    }

    // ─── Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("No stranded events")
    class NoStrandedEvents {

        @Test
        @DisplayName("Should do nothing when no stranded events found")
        void shouldDoNothingWhenEmpty() {
            // Arrange
            when(eventStoreService.findStrandedEvents()).thenReturn(List.of());

            // Act
            recoveryJob.recoverStrandedEvents();

            // Assert — no publish attempts, no status updates
            verifyNoInteractions(rabbitTemplate);
        }
    }

    @Nested
    @DisplayName("Successful recovery")
    class SuccessfulRecovery {

        @Test
        @DisplayName("Should re-publish stranded PENDING event and mark as PUBLISHED")
        void shouldRecoverPendingEvent() throws Exception {
            // Arrange
            var strandedEvent = createStrandedEvent(1L, "StaffMemberAddedEvent", "PENDING", 0);
            when(eventStoreService.findStrandedEvents()).thenReturn(List.of(strandedEvent));

            var mockEvent = mock(StaffMemberAddedEvent.class);
            when(objectMapper.readValue(eq(strandedEvent.getEventBody()), any(Class.class)))
                    .thenReturn(mockEvent);
            when(rabbitOutboxRouter.resolve(mockEvent))
                    .thenReturn(new RabbitOutboxRouter.Destination("staff-management", "staff.member.added"));

            // Act
            recoveryJob.recoverStrandedEvents();

            // Assert
            verify(rabbitTemplate).convertAndSend("staff-management", "staff.member.added", mockEvent);
            verify(eventStoreService).updateStatus(1L,
                    EventStoreService.StatusOfMessageDelivery.PUBLISHED, false);
        }
    }

    @Nested
    @DisplayName("Failure handling")
    class FailureHandling {

        @Test
        @DisplayName("Should increment retry count when broker is unavailable")
        void shouldIncrementRetryOnBrokerFailure() throws Exception {
            // Arrange
            var strandedEvent = createStrandedEvent(2L, "StaffMemberAddedEvent", "PENDING", 1);
            when(eventStoreService.findStrandedEvents()).thenReturn(List.of(strandedEvent));

            var mockEvent = mock(StaffMemberAddedEvent.class);
            when(objectMapper.readValue(eq(strandedEvent.getEventBody()), any(Class.class)))
                    .thenReturn(mockEvent);
            when(rabbitOutboxRouter.resolve(mockEvent))
                    .thenReturn(new RabbitOutboxRouter.Destination("staff-management", "staff.member.added"));
            doThrow(new AmqpException("Broker down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // Act
            recoveryJob.recoverStrandedEvents();

            // Assert — status stays PENDING, retry incremented
            verify(eventStoreService).updateStatus(2L,
                    EventStoreService.StatusOfMessageDelivery.PENDING, true);
        }

        @Test
        @DisplayName("Should skip events exceeding max recovery retries")
        void shouldSkipExcessiveRetries() {
            // Arrange — event has 10 retries (equals MAX_RECOVERY_RETRIES)
            var strandedEvent = createStrandedEvent(3L, "StaffMemberAddedEvent", "FAILED", 10);
            when(eventStoreService.findStrandedEvents()).thenReturn(List.of(strandedEvent));

            // Act
            recoveryJob.recoverStrandedEvents();

            // Assert — no publish attempted, no status change
            verifyNoInteractions(rabbitTemplate);
            verify(eventStoreService, never()).updateStatus(eq(3L), any(), anyBoolean());
        }

        @Test
        @DisplayName("Should mark unknown event type as FAILED")
        void shouldHandleUnknownEventType() {
            // Arrange — event type class does not exist
            var strandedEvent = createStrandedEvent(4L, "NonExistentEvent", "PENDING", 0);
            when(eventStoreService.findStrandedEvents()).thenReturn(List.of(strandedEvent));

            // Act
            recoveryJob.recoverStrandedEvents();

            // Assert — marked as FAILED so it stops being polled
            verify(eventStoreService).updateStatus(4L,
                    EventStoreService.StatusOfMessageDelivery.FAILED, true);
            verifyNoInteractions(rabbitTemplate);
        }
    }
}
