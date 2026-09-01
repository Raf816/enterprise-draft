package com.staffs.leavebooking.integration;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.EventStoreService;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService;
import com.staffs.leavebooking.leavemanagement.application.listeners.LeaveRequestSubmittedListener;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveAllowanceRepository;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving BEFORE_COMMIT listener behaviour with real transactions.
 *
 * <p><strong>Why a separate class?</strong> The main {@link LeaveRequestIntegrationTest}
 * uses {@code @DataJpaTest} which wraps each test in a transaction that rolls back.
 * BEFORE_COMMIT listeners need the transaction to actually commit (or fail) to prove
 * they work. This class disables the automatic test transaction via
 * {@code @Transactional(propagation = NOT_SUPPORTED)}, then uses {@link TransactionTemplate}
 * to create real independent transactions that commit or roll back naturally.
 *
 * <p><strong>What these tests prove:</strong>
 * <ul>
 *   <li>Successful submission: leave request AND daysPending reservation commit atomically</li>
 *   <li>Insufficient allowance: BEFORE_COMMIT listener fails → entire transaction rolls back
 *       → no PENDING request persisted, allowance unchanged</li>
 * </ul>
 */
@DataJpaTest
@Import({
        LeaveRequestApplicationService.class,
        LeaveAllowanceApplicationService.class,
        DomainEventManager.class,
        EventStoreService.class,
        JacksonAutoConfiguration.class,
        LeaveRequestSubmittedListener.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable auto test transaction
@DisplayName("Atomic Allowance Consistency (BEFORE_COMMIT)")
class AtomicAllowanceConsistencyIntegrationTest {

    @Autowired
    private LeaveRequestApplicationService leaveRequestService;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String STAFF_ID = "atomic-test-staff";
    private static final String MANAGER_ID = "atomic-test-mgr";

    @BeforeEach
    void setUp() {
        // Clean up in its own transaction (no auto-transaction)
        transactionTemplate.executeWithoutResult(status -> {
            leaveRequestRepository.findByStaffMemberId(STAFF_ID)
                    .forEach(leaveRequestRepository::delete);
            leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_ID)
                    .ifPresent(leaveAllowanceRepository::delete);
        });
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        transactionTemplate.executeWithoutResult(status -> {
            leaveRequestRepository.findByStaffMemberId(STAFF_ID)
                    .forEach(leaveRequestRepository::delete);
            leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_ID)
                    .ifPresent(leaveAllowanceRepository::delete);
        });
    }

    @Test
    @DisplayName("Sufficient balance: request + daysPending committed atomically")
    void shouldCommitRequestAndReserveDaysAtomically() {
        // Arrange — create allowance with 25 days
        transactionTemplate.executeWithoutResult(status -> {
            LeaveAllowanceJpa allowance = createAllowance(25, 0, 0);
            leaveAllowanceRepository.save(allowance);
        });

        // Act — submit a 5-day request (Mon-Fri)
        LocalDate start = findNextMonday().plusWeeks(20);
        LocalDate end = start.plusDays(4);

        transactionTemplate.executeWithoutResult(status -> {
            leaveRequestService.submitNewRequest(new SubmitLeaveRequestCommand(
                    STAFF_ID, MANAGER_ID, start, end, "ANNUAL", "atomic-success"));
        });

        // Assert — both committed in the same transaction
        LeaveAllowanceJpa allowance = leaveAllowanceRepository
                .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_ID)
                .orElseThrow();
        assertThat(allowance.getDaysPending()).isEqualTo(5);

        List<LeaveRequestJpa> requests = leaveRequestRepository.findByStaffMemberId(STAFF_ID);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(requests.get(0).getReason()).isEqualTo("atomic-success");
    }

    @Test
    @DisplayName("Insufficient balance: transaction rolls back, no request persisted")
    void shouldRollBackWhenInsufficientAllowance() {
        // Arrange — create allowance with only 2 days
        transactionTemplate.executeWithoutResult(status -> {
            LeaveAllowanceJpa allowance = createAllowance(2, 0, 0);
            leaveAllowanceRepository.save(allowance);
        });

        // Act — try to submit a 5-day request (only 2 available)
        LocalDate start = findNextMonday().plusWeeks(25);
        LocalDate end = start.plusDays(4);

        assertThrows(IllegalStateException.class, () ->
                transactionTemplate.executeWithoutResult(status -> {
                    leaveRequestService.submitNewRequest(new SubmitLeaveRequestCommand(
                            STAFF_ID, MANAGER_ID, start, end, "ANNUAL", "atomic-fail"));
                })
        );

        // Assert — nothing persisted, allowance unchanged
        List<LeaveRequestJpa> requests = leaveRequestRepository.findByStaffMemberId(STAFF_ID);
        assertThat(requests.stream().noneMatch(r -> "atomic-fail".equals(r.getReason()))).isTrue();

        LeaveAllowanceJpa allowance = leaveAllowanceRepository
                .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(STAFF_ID)
                .orElseThrow();
        assertThat(allowance.getDaysPending()).isEqualTo(0);
        assertThat(allowance.getTotalEntitlement()).isEqualTo(2);
    }

    // ─── Helpers ───

    private LeaveAllowanceJpa createAllowance(int entitlement, int used, int pending) {
        LeaveAllowanceJpa a = new LeaveAllowanceJpa();
        a.setId(UUID.randomUUID().toString());
        a.setStaffMemberId(STAFF_ID);
        a.setManagerId(MANAGER_ID);
        a.setFirstName("Atomic");
        a.setSurname("Test");
        a.setDepartment("Engineering");
        a.setBusinessYearStart(LocalDate.now().getYear());
        a.setBusinessYearEnd(LocalDate.now().getYear() + 1);
        a.setTotalEntitlement(entitlement);
        a.setDaysUsed(used);
        a.setDaysPending(pending);
        return a;
    }

    private LocalDate findNextMonday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() != 1) date = date.plusDays(1);
        return date;
    }
}
