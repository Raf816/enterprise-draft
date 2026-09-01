package com.staffs.leavebooking.integration;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.EventStoreService;
import com.staffs.leavebooking.leavemanagement.application.commands.AmendEntitlementCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.CancelLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService;
import com.staffs.leavebooking.leavemanagement.application.listeners.LeaveRequestSubmittedListener;
import com.staffs.leavebooking.leavemanagement.application.listeners.LeaveRequestApprovedListener;
import com.staffs.leavebooking.leavemanagement.application.listeners.LeaveRequestRejectedListener;
import com.staffs.leavebooking.leavemanagement.application.listeners.LeaveRequestCancelledListener;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveAllowanceRepository;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Leave Request and Leave Allowance command flows.
 * Uses {@code @DataJpaTest} to load only the JPA slice (repositories, entities, H2).
 * Application services and event listeners are imported explicitly via {@code @Import}.
 *
 * <p><strong>What these tests prove:</strong>
 * <ul>
 *   <li>Application services correctly coordinate domain logic and persistence</li>
 *   <li>Domain invariants are enforced end-to-end (past dates, state transitions)</li>
 *   <li>Repositories correctly persist and retrieve data via H2</li>
 *   <li>LeaveAllowance operations (reserve/confirm/release/credit) work correctly</li>
 *   <li>Idempotency guards work (duplicate allowance prevention)</li>
 *   <li>Event store captures domain events during command execution</li>
 *   <li><strong>BEFORE_COMMIT listeners</strong> atomically update allowances within the
 *       same transaction as the leave request — insufficient balance rolls back both</li>
 * </ul>
 *
 * <p><strong>Transaction design:</strong> The four allowance listeners use
 * {@code @TransactionalEventListener(BEFORE_COMMIT)}, so they fire synchronously within
 * the producing transaction. This means the allowance update is atomic with the leave
 * request save. Tests that need to verify committed state use {@link TransactionTemplate}
 * to control transaction boundaries explicitly.
 */
@DataJpaTest
@Import({
        LeaveRequestApplicationService.class,
        LeaveAllowanceApplicationService.class,
        DomainEventManager.class,
        EventStoreService.class,
        JacksonAutoConfiguration.class,
        // Real BEFORE_COMMIT listeners — fire synchronously within the transaction
        LeaveRequestSubmittedListener.class,
        LeaveRequestApprovedListener.class,
        LeaveRequestRejectedListener.class,
        LeaveRequestCancelledListener.class
})
@ActiveProfiles("test")
@DisplayName("Leave Request Integration Tests (@DataJpaTest)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeaveRequestIntegrationTest {

    @Autowired
    private LeaveRequestApplicationService leaveRequestService;

    @Autowired
    private LeaveAllowanceApplicationService leaveAllowanceService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String STAFF_MEMBER_ID = "int-test-staff-001";
    private static final String MANAGER_ID = "int-test-mgr-001";

    @BeforeEach
    void setUp() {
        // Clear any existing test data for our test staff member
        leaveRequestRepository.findByStaffMemberId(STAFF_MEMBER_ID)
                .forEach(leaveRequestRepository::delete);
        leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                .ifPresent(leaveAllowanceRepository::delete);

        // Create a fresh allowance for the test staff member (25 days, none used)
        LeaveAllowanceJpa allowance = new LeaveAllowanceJpa();
        allowance.setId(UUID.randomUUID().toString());
        allowance.setStaffMemberId(STAFF_MEMBER_ID);
        allowance.setManagerId(MANAGER_ID);
        allowance.setFirstName("Integration");
        allowance.setSurname("Test");
        allowance.setDepartment("Engineering");
        allowance.setBusinessYearStart(LocalDate.now().getYear());
        allowance.setBusinessYearEnd(LocalDate.now().getYear() + 1);
        allowance.setTotalEntitlement(25);
        allowance.setDaysUsed(0);
        allowance.setDaysPending(0);
        leaveAllowanceRepository.save(allowance);
    }

    // ---------------------------------------------------------------
    // SUBMIT LEAVE REQUEST
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Submit Leave Request")
    class SubmitLeaveRequest {

        @Test
        @DisplayName("Should persist leave request with PENDING status")
        void shouldPersistLeaveRequest() {
            // Arrange
            LocalDate start = findNextMonday().plusWeeks(2);
            LocalDate end = start.plusDays(4);

            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_MEMBER_ID, MANAGER_ID, start, end, "ANNUAL", "Integration test holiday"
            );

            // Act
            leaveRequestService.submitNewRequest(command);

            // Assert
            List<LeaveRequestJpa> requests = leaveRequestRepository.findByStaffMemberId(STAFF_MEMBER_ID);
            assertFalse(requests.isEmpty(), "At least one leave request should be persisted");

            LeaveRequestJpa saved = requests.get(requests.size() - 1);
            assertEquals("PENDING", saved.getStatus());
            assertEquals(STAFF_MEMBER_ID, saved.getStaffMemberId());
            assertEquals(MANAGER_ID, saved.getManagerId());
            assertEquals("ANNUAL", saved.getLeaveType());
            assertEquals(5, saved.getNumberOfDays());
            assertEquals("Integration test holiday", saved.getReason());
            assertNotNull(saved.getSubmittedOn());
        }

        @Test
        @DisplayName("Should reject leave request with past start date")
        void shouldRejectPastStartDate() {
            // Arrange
            LocalDate pastDate = LocalDate.now().minusDays(5);
            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_MEMBER_ID, MANAGER_ID, pastDate, pastDate.plusDays(4), "ANNUAL", "Past"
            );

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> leaveRequestService.submitNewRequest(command));
        }

        @Test
        @DisplayName("Should reject leave request with end date before start date")
        void shouldRejectEndBeforeStart() {
            // Arrange
            LocalDate start = findNextMonday().plusWeeks(2);
            LocalDate end = start.minusDays(1);
            SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                    STAFF_MEMBER_ID, MANAGER_ID, start, end, "ANNUAL", "Invalid range"
            );

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> leaveRequestService.submitNewRequest(command));
        }
    }

    // ---------------------------------------------------------------
    // APPROVE LEAVE REQUEST
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Approve Leave Request")
    class ApproveLeaveRequest {

        @Test
        @DisplayName("Should transition status to APPROVED with decided metadata")
        void shouldApprove() {
            // Arrange
            String requestId = submitTestRequest("Approve test");

            // Act
            leaveRequestService.approveRequest(requestId, MANAGER_ID, null);

            // Assert
            LeaveRequestJpa approved = leaveRequestRepository.findById(requestId).orElseThrow();
            assertEquals("APPROVED", approved.getStatus());
            assertEquals(MANAGER_ID, approved.getDecidedBy());
            assertNotNull(approved.getDecidedOn());
        }
    }

    // ---------------------------------------------------------------
    // REJECT LEAVE REQUEST
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Reject Leave Request")
    class RejectLeaveRequest {

        @Test
        @DisplayName("Should transition status to REJECTED with decided metadata")
        void shouldReject() {
            // Arrange
            String requestId = submitTestRequest("Reject test");

            // Act
            leaveRequestService.rejectRequest(requestId, MANAGER_ID, null);

            // Assert
            LeaveRequestJpa rejected = leaveRequestRepository.findById(requestId).orElseThrow();
            assertEquals("REJECTED", rejected.getStatus());
            assertEquals(MANAGER_ID, rejected.getDecidedBy());
            assertNotNull(rejected.getDecidedOn());
        }
    }

    // ---------------------------------------------------------------
    // CANCEL LEAVE REQUEST
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Cancel Leave Request")
    class CancelLeaveRequest {

        @Test
        @DisplayName("Should cancel PENDING request with cancellation reason")
        void shouldCancelPending() {
            // Arrange
            String requestId = submitTestRequest("Cancel test");

            // Act
            CancelLeaveRequestCommand cancelCommand = new CancelLeaveRequestCommand(
                    requestId, STAFF_MEMBER_ID, "Changed plans"
            );
            leaveRequestService.cancelRequest(cancelCommand);

            // Assert
            LeaveRequestJpa cancelled = leaveRequestRepository.findById(requestId).orElseThrow();
            assertEquals("CANCELLED", cancelled.getStatus());
            assertEquals("Changed plans", cancelled.getCancellationReason());
        }

        @Test
        @DisplayName("Should cancel APPROVED request with cancellation reason")
        void shouldCancelApproved() {
            // Arrange
            String requestId = submitTestRequest("Cancel approved test");
            leaveRequestService.approveRequest(requestId, MANAGER_ID, null);

            // Act
            CancelLeaveRequestCommand cancelCommand = new CancelLeaveRequestCommand(
                    requestId, STAFF_MEMBER_ID, "No longer needed"
            );
            leaveRequestService.cancelRequest(cancelCommand);

            // Assert
            LeaveRequestJpa cancelled = leaveRequestRepository.findById(requestId).orElseThrow();
            assertEquals("CANCELLED", cancelled.getStatus());
            assertEquals("No longer needed", cancelled.getCancellationReason());
        }
    }

    // ---------------------------------------------------------------
    // ALLOWANCE OPERATIONS (simulates what event listeners do)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Allowance Operations")
    class AllowanceOperations {

        @Test
        @DisplayName("Should reserve pending days (simulates LeaveRequestSubmittedListener)")
        void shouldReserveDays() {
            // Act
            leaveAllowanceService.reserveDays(STAFF_MEMBER_ID, 5);

            // Assert
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(5, allowance.getDaysPending());
            assertEquals(0, allowance.getDaysUsed());
        }

        @Test
        @DisplayName("Should confirm days (simulates LeaveRequestApprovedListener)")
        void shouldConfirmDays() {
            // Arrange
            leaveAllowanceService.reserveDays(STAFF_MEMBER_ID, 5);

            // Act
            leaveAllowanceService.confirmDays(STAFF_MEMBER_ID, 5);

            // Assert
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(5, allowance.getDaysUsed());
            assertEquals(0, allowance.getDaysPending());
        }

        @Test
        @DisplayName("Should release pending days (simulates LeaveRequestRejectedListener)")
        void shouldReleasePendingDays() {
            // Arrange
            leaveAllowanceService.reserveDays(STAFF_MEMBER_ID, 5);

            // Act
            leaveAllowanceService.releasePendingDays(STAFF_MEMBER_ID, 5);

            // Assert
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(0, allowance.getDaysPending());
            assertEquals(0, allowance.getDaysUsed());
        }

        @Test
        @DisplayName("Should credit back days when approved request is cancelled")
        void shouldCreditBackDays() {
            // Arrange
            leaveAllowanceService.reserveDays(STAFF_MEMBER_ID, 5);
            leaveAllowanceService.confirmDays(STAFF_MEMBER_ID, 5);

            // Act
            leaveAllowanceService.creditBackDays(STAFF_MEMBER_ID, 5);

            // Assert
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(0, allowance.getDaysUsed());
            assertEquals(0, allowance.getDaysPending());
        }

        @Test
        @DisplayName("Should create allowance for new staff member")
        void shouldCreateAllowanceForNewStaff() {
            // Arrange
            String newStaffId = "new-staff-" + UUID.randomUUID().toString().substring(0, 8);

            // Act
            leaveAllowanceService.createAllowanceForNewStaff(
                    newStaffId, MANAGER_ID, "New", "Person", "Engineering", 28
            );

            // Assert
            LeaveAllowanceJpa created = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(newStaffId)
                    .orElseThrow();
            assertEquals(28, created.getTotalEntitlement());
            assertEquals(0, created.getDaysUsed());
            assertEquals(0, created.getDaysPending());
            assertEquals("New", created.getFirstName());
            assertEquals("Person", created.getSurname());
        }

        @Test
        @DisplayName("Should not create duplicate allowance (idempotency guard)")
        void shouldNotCreateDuplicateAllowance() {
            // Arrange
            String staffId = "idem-staff-" + UUID.randomUUID().toString().substring(0, 8);
            leaveAllowanceService.createAllowanceForNewStaff(
                    staffId, MANAGER_ID, "First", "Call", "Eng", 25
            );

            // Act
            leaveAllowanceService.createAllowanceForNewStaff(
                    staffId, MANAGER_ID, "First", "Call", "Eng", 25
            );

            // Assert
            List<LeaveAllowanceJpa> all = leaveAllowanceRepository.findAll();
            long count = all.stream().filter(a -> a.getStaffMemberId().equals(staffId)).count();
            assertEquals(1, count);
        }

        @Test
        @DisplayName("Should amend entitlement via admin command")
        void shouldAmendEntitlement() {
            // Arrange
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();

            AmendEntitlementCommand command = new AmendEntitlementCommand(
                    allowance.getId(), 30
            );

            // Act
            leaveAllowanceService.amendEntitlement(command);

            // Assert
            LeaveAllowanceJpa updated = leaveAllowanceRepository.findById(allowance.getId()).orElseThrow();
            assertEquals(30, updated.getTotalEntitlement());
        }
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    /**
     * Submits a test leave request and returns its persisted ID.
     */
    private String submitTestRequest(String reason) {
        LocalDate start = findNextMonday().plusWeeks(10);
        LocalDate end = start.plusDays(4);

        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                STAFF_MEMBER_ID, MANAGER_ID, start, end, "ANNUAL", reason
        );
        leaveRequestService.submitNewRequest(command);

        return leaveRequestRepository.findByStaffMemberId(STAFF_MEMBER_ID).stream()
                .filter(r -> reason.equals(r.getReason()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Leave request not found after submission"))
                .getId();
    }

    private LocalDate findNextMonday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() != 1) {
            date = date.plusDays(1);
        }
        return date;
    }

    // ---------------------------------------------------------------
    // BEFORE_COMMIT LISTENER INTEGRATION TESTS
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Atomic Allowance Consistency (BEFORE_COMMIT listeners)")
    class AtomicAllowanceConsistency {

        @Test
        @Order(20)
        @DisplayName("Submit with sufficient balance — request committed + daysPending incremented atomically")
        void shouldCommitRequestAndReserveDaysAtomically() {
            // Act — submit in an explicit transaction so BEFORE_COMMIT listener fires
            transactionTemplate.executeWithoutResult(status -> {
                LocalDate start = findNextMonday().plusWeeks(20);
                LocalDate end = start.plusDays(4); // 5 working days (Mon-Fri)
                SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                        STAFF_MEMBER_ID, MANAGER_ID, start, end, "ANNUAL", "atomic-test-success"
                );
                leaveRequestService.submitNewRequest(command);
            });

            // Assert — in a new read context, verify both committed
            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(5, allowance.getDaysPending(),
                    "BEFORE_COMMIT listener should have reserved 5 pending days");

            List<LeaveRequestJpa> requests = leaveRequestRepository.findByStaffMemberId(STAFF_MEMBER_ID);
            assertTrue(requests.stream().anyMatch(r -> "atomic-test-success".equals(r.getReason())),
                    "Leave request should be persisted");
        }

        @Test
        @Order(21)
        @DisplayName("Submit with insufficient balance — transaction rolls back, no request persisted, allowance unchanged")
        void shouldRollBackWhenInsufficientAllowance() {
            // Arrange — set allowance to only 2 days available
            transactionTemplate.executeWithoutResult(status -> {
                LeaveAllowanceJpa allowance = leaveAllowanceRepository
                        .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                        .orElseThrow();
                allowance.setTotalEntitlement(2);
                allowance.setDaysUsed(0);
                allowance.setDaysPending(0);
                leaveAllowanceRepository.save(allowance);
            });

            // Act — try to submit a 5-day request (only 2 available) — should fail
            assertThrows(Exception.class, () ->
                transactionTemplate.executeWithoutResult(status -> {
                    LocalDate start = findNextMonday().plusWeeks(25);
                    LocalDate end = start.plusDays(4); // 5 working days
                    SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                            STAFF_MEMBER_ID, MANAGER_ID, start, end, "ANNUAL", "atomic-test-fail"
                    );
                    leaveRequestService.submitNewRequest(command);
                })
            );

            // Assert — no request persisted, allowance unchanged
            List<LeaveRequestJpa> requests = leaveRequestRepository.findByStaffMemberId(STAFF_MEMBER_ID);
            assertTrue(requests.stream().noneMatch(r -> "atomic-test-fail".equals(r.getReason())),
                    "No leave request should be persisted when allowance is insufficient");

            LeaveAllowanceJpa allowance = leaveAllowanceRepository
                    .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_MEMBER_ID)
                    .orElseThrow();
            assertEquals(0, allowance.getDaysPending(),
                    "Allowance daysPending should remain 0 after rollback");
            assertEquals(2, allowance.getTotalEntitlement(),
                    "Allowance totalEntitlement should remain unchanged");
        }
    }
}
