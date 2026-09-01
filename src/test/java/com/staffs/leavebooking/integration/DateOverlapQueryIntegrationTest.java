package com.staffs.leavebooking.integration;

import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test proving the custom @Query date-overlap JPQL works correctly
 * against a real H2 database.
 *
 * <p>The overlap condition is: {@code startDate <= :to AND endDate >= :from}.
 * This catches requests that:
 * <ul>
 *   <li>Fall completely inside the search range</li>
 *   <li>Span the start boundary (start before range, end within)</li>
 *   <li>Span the end boundary (start within range, end after)</li>
 *   <li>Span the entire range (start before, end after)</li>
 * </ul>
 * And correctly excludes requests completely outside the range.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Date Overlap Query Integration Tests")
class DateOverlapQueryIntegrationTest {

    @Autowired
    private LeaveRequestRepository repository;

    private static final String STAFF_ID = "staff-overlap-test";
    private static final String MANAGER_ID = "mgr-overlap-test";

    // Search range: September 2026 (1st to 30th)
    private static final LocalDate SEARCH_FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEARCH_TO = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Request A: completely INSIDE the range (Sep 10-15)
        repository.save(createRequest("inside", STAFF_ID,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 15), "PENDING"));

        // Request B: SPANS START boundary (Aug 28 - Sep 5)
        repository.save(createRequest("span-start", STAFF_ID,
                LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 5), "APPROVED"));

        // Request C: SPANS END boundary (Sep 25 - Oct 3)
        repository.save(createRequest("span-end", STAFF_ID,
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 3), "PENDING"));

        // Request D: SPANS ENTIRE range (Aug 15 - Oct 15)
        repository.save(createRequest("span-all", STAFF_ID,
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 15), "APPROVED"));

        // Request E: completely OUTSIDE — before range (Aug 1 - Aug 20)
        repository.save(createRequest("before", STAFF_ID,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20), "PENDING"));

        // Request F: completely OUTSIDE — after range (Oct 10 - Oct 20)
        repository.save(createRequest("after", STAFF_ID,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20), "PENDING"));
    }

    @Nested
    @DisplayName("findByStaffMemberIdAndDateOverlap")
    class StaffOverlap {

        @Test
        @DisplayName("Should return 4 overlapping requests and exclude 2 non-overlapping")
        void shouldFindAllOverlappingRequests() {
            List<LeaveRequestJpa> results = repository.findByStaffMemberIdAndDateOverlap(
                    STAFF_ID, SEARCH_FROM, SEARCH_TO);

            assertThat(results).hasSize(4);
            assertThat(results).extracting(LeaveRequestJpa::getId)
                    .containsExactlyInAnyOrder("inside", "span-start", "span-end", "span-all");
        }
    }

    @Nested
    @DisplayName("findByStaffMemberIdAndStatusAndDateOverlap")
    class StaffStatusOverlap {

        @Test
        @DisplayName("Should filter by both status and date overlap")
        void shouldFilterByStatusAndOverlap() {
            // Only PENDING overlapping requests (inside + span-end = 2)
            List<LeaveRequestJpa> results = repository.findByStaffMemberIdAndStatusAndDateOverlap(
                    STAFF_ID, "PENDING", SEARCH_FROM, SEARCH_TO);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(LeaveRequestJpa::getId)
                    .containsExactlyInAnyOrder("inside", "span-end");
        }
    }

    @Nested
    @DisplayName("findByManagerIdAndDateOverlap")
    class ManagerOverlap {

        @Test
        @DisplayName("Should find overlapping requests by manager")
        void shouldFindByManagerAndOverlap() {
            List<LeaveRequestJpa> results = repository.findByManagerIdAndDateOverlap(
                    MANAGER_ID, SEARCH_FROM, SEARCH_TO);

            assertThat(results).hasSize(4);
        }
    }

    @Nested
    @DisplayName("findByDateOverlap (company-wide)")
    class CompanyWideOverlap {

        @Test
        @DisplayName("Should find all overlapping requests regardless of person")
        void shouldFindAllOverlapping() {
            List<LeaveRequestJpa> results = repository.findByDateOverlap(SEARCH_FROM, SEARCH_TO);

            assertThat(results).hasSize(4);
        }

        @Test
        @DisplayName("Should find zero overlapping for a range with no requests")
        void shouldFindNoneForEmptyRange() {
            List<LeaveRequestJpa> results = repository.findByDateOverlap(
                    LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31));

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatusAndDateOverlap")
    class StatusOverlap {

        @Test
        @DisplayName("Should filter by status and date overlap company-wide")
        void shouldFilterByStatusCompanyWide() {
            // APPROVED + overlapping = span-start + span-all = 2
            List<LeaveRequestJpa> results = repository.findByStatusAndDateOverlap(
                    "APPROVED", SEARCH_FROM, SEARCH_TO);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(LeaveRequestJpa::getId)
                    .containsExactlyInAnyOrder("span-start", "span-all");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────

    private LeaveRequestJpa createRequest(String id, String staffId,
                                           LocalDate startDate, LocalDate endDate,
                                           String status) {
        LeaveRequestJpa jpa = new LeaveRequestJpa();
        jpa.setId(id);
        jpa.setStaffMemberId(staffId);
        jpa.setManagerId(MANAGER_ID);
        jpa.setLeaveType("ANNUAL");
        jpa.setStartDate(startDate);
        jpa.setEndDate(endDate);
        jpa.setNumberOfDays(5); // placeholder
        jpa.setStatus(status);
        jpa.setSubmittedOn(LocalDate.now());
        return jpa;
    }
}
