# Testing Strategy Design — Leave Booking System

**Module:** COMP60047 Enterprise Application Development
**Assignment:** Scenario 1 — Leave Booking System
**Last Updated:** 2026-09-01

---

## 1. Testing Pyramid

```
           ┌─────────────┐
           │   API Tests  │  ← Postman collections (139 requests)
           │  (Manual/E2E)│     Full HTTP, JWT, role-based
           └──────┬───────┘
              ┌───┴────────────┐
              │ Integration    │  ← @DataJpaTest + @Import
              │ Tests (23)     │     Service → Domain → H2
              └──────┬─────────┘
         ┌───────────┴──────────────┐
         │    Unit Tests (444)      │  ← Plain JUnit 5 + Mockito
         │  Domain, Mappers,        │     No Spring context, fast
         │  Handlers, Listeners,    │
         │  Services                │
         └──────────────────────────┘
```

**Philosophy:** The widest layer is pure unit tests (fast, isolated, no Spring context). Integration tests use a thin JPA slice with H2. API tests use Postman against the running application.

---

## 2. Unit Test Layer

### 2.1 What Is Tested

| Package | Classes Tested | Tests | Pattern |
|---------|---------------|-------|---------|
| `common.domain` | DomainAssertions, Email, FullName, Identity | ~57 | Invariant guards |
| `leavemanagement.domain` | BusinessYear, DateRange, LeaveReason, LeaveAllowance, LeaveRequest | ~100 | State machines, invariants, events |
| `staffmanagement.domain` | StaffMember | ~27 | Factory, mutations, events |
| `leavemanagement.application.mappers` | 6 mapper classes | ~27 | Bidirectional mapping |
| `staffmanagement.application.mappers` | 3 mapper classes | ~14 | Bidirectional mapping |
| `leavemanagement.application.handlers` | Query handlers | ~12 | Mocked repos, delegation |
| `leavemanagement.application.listeners` | 6 event listeners | ~7 | Mocked services, verify calls |
| `staffmanagement.application.handlers` | StaffApplicationService, StaffQueryHandler | ~13 | Mocked repos + events |
| `common.events` | EventStoreService | ~5 | Mocked repo + ObjectMapper |

### 2.2 Patterns Used

**AAA (Arrange / Act / Assert)** — from Lecture 2:
```java
@Test
@DisplayName("Should create a PENDING leave request with correct fields")
void shouldCreatePendingRequest() {
    // Arrange
    Identity<LeaveRequest> id = Identity.generateId();
    DateRange dateRange = LeaveRequestMother.futureDateRange(7, 5);

    // Act
    LeaveRequest request = LeaveRequest.submitNew(
            id, STAFF_ID, MANAGER_ID, LeaveType.ANNUAL, dateRange, "Holiday");

    // Assert
    assertEquals(LeaveRequestStatus.PENDING, request.status());
    assertEquals(LocalDate.now(), request.submittedOn());
}
```

**Object Mother** — reusable factory methods for test objects:
```java
public class LeaveRequestMother {
    public static final String STAFF_MEMBER_ID = "staff-001";
    public static final String MANAGER_ID = "mgr-001";

    public static DateRange futureDateRange(int daysFromNow, int duration) {
        LocalDate start = LocalDate.now().plusDays(daysFromNow);
        return new DateRange(start, start.plusDays(duration - 1));
    }

    public static LeaveRequest createPendingRequest() {
        return LeaveRequest.submitNew(
            Identity.generateId(), STAFF_MEMBER_ID, MANAGER_ID,
            LeaveType.ANNUAL, futureDateRange(7, 5), "Test reason");
    }
}
```

**@Nested + @DisplayName** — readable test organisation:
```java
@DisplayName("LeaveRequest Aggregate Root")
class LeaveRequestTest {

    @Nested
    @DisplayName("submitNew() factory method")
    class SubmitNew { ... }

    @Nested
    @DisplayName("approve() command")
    class Approve { ... }

    @Nested
    @DisplayName("cancel() command")
    class Cancel { ... }
}
```

**Mockito for service/handler tests:**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequest Query Handler")
class LeaveRequestQueryHandlerTest {

    @Mock private LeaveRequestRepository repository;
    @InjectMocks private LeaveRequestQueryHandler handler;

    @Test
    @DisplayName("Should return all requests for a staff member")
    void shouldReturnRequestsForStaff() {
        // Arrange
        when(repository.findByStaffMemberId("staff-1"))
                .thenReturn(List.of(createTestJpa()));
        // Act
        var result = handler.findByStaffMemberId("staff-1");
        // Assert
        assertEquals(1, result.size());
        verify(repository).findByStaffMemberId("staff-1");
    }
}
```

### 2.3 Properties of Good Unit Tests (Lecture 2)

| Property | How We Achieve It |
|----------|-------------------|
| **Fast** | No Spring context, no database, no network — pure Java + Mockito |
| **Isolated** | Each test creates its own objects, @BeforeEach resets state |
| **Repeatable** | No randomness, deterministic date calculations, no shared mutable state |
| **Self-validating** | JUnit assertions — pass/fail is automatic |
| **Thorough** | Test happy paths AND guard conditions (nulls, invalid ranges, wrong states) |

---

## 3. Integration Test Layer

### 3.1 Approach: @DataJpaTest + @Import

```java
@DataJpaTest
@Import({
    LeaveRequestApplicationService.class,
    LeaveAllowanceApplicationService.class,
    DomainEventManager.class,
    EventStoreService.class,
    JacksonAutoConfiguration.class
})
@ActiveProfiles("test")
class LeaveRequestIntegrationTest { ... }
```

### 3.2 Why @DataJpaTest (Not @SpringBootTest)

| Issue | @SpringBootTest | @DataJpaTest + @Import |
|-------|----------------|------------------------|
| Firebase | Tries to load serviceAccountKey.json | Not loaded |
| RabbitMQ | Tries to connect to broker | Not loaded |
| Security | Requires JWT configuration | Not loaded |
| Speed | Full context: 15-30 seconds | JPA slice: ~6 seconds |
| Scope | Everything loaded, hard to isolate | Only what's imported |

### 3.3 What Integration Tests Prove

| Test | What's Proved |
|------|---------------|
| Submit → persist with PENDING status | Service → Domain → Mapper → Repository → H2 |
| Reject past dates | Domain invariant enforced end-to-end |
| Approve transitions to APPROVED | State machine works through service layer |
| Cancel from PENDING and APPROVED | Both cancel paths work |
| reserveDays / confirmDays / releasePendingDays / creditBackDays | Allowance arithmetic correct via service |
| createAllowanceForNewStaff (idempotent) | Idempotency guard works with real DB |
| amendEntitlement | Admin operation persists correctly |

### 3.4 Event Listener Consideration

The local allowance event listeners use `@TransactionalEventListener(BEFORE_COMMIT)` (synchronous, same transaction). However, `@DataJpaTest` wraps each test in a rollback-only transaction that never reaches the commit phase — so `BEFORE_COMMIT` listeners do not fire during standard `@DataJpaTest` tests. Instead we:
1. Test the **service methods** that listeners call (reserveDays, etc.) — proves the logic
2. Test the **listeners themselves** with Mockito — proves correct delegation
3. Test **atomic commit/rollback** in `AtomicAllowanceConsistencyIntegrationTest` — uses `@Transactional(propagation = NOT_SUPPORTED)` with `TransactionTemplate` for real independent transactions

Together these three approaches provide complete proof that the event flow works.

---

## 4. API Test Layer (Postman)

### 4.1 Purpose
Test the full HTTP request/response cycle including:
- Authentication (Firebase JWT tokens)
- Authorisation (role-based access control)
- Request validation
- Error responses (401, 403, 404, 400)
- End-to-end event flows (POST /staff → RabbitMQ → leave_allowance created)

### 4.2 Collections (Implemented)
- 8 domain-based folders with Edge Cases subfolders
- 139 requests per collection (automated + manual)
- Identity (register/login/role-check/find-user/password)
- Staff Management (skeleton setup, queries, search, POST /staff, PATCH)
- Leave Requests (submit, approve/reject/cancel, queries, search)
- Leave Allowances (view/amend)
- Comprehensive edge cases: 401, 403, 404, 400 validation, 409 state conflicts

### 4.3 Token Management
```javascript
// Postman Scripts tab on login request:
const response = pm.response.json();
pm.globals.set("jwt_admin_token", response.accessToken);
```

---

## 5. Design Decisions

| Decision | Reason |
|----------|--------|
| Identity module testing | Firebase is external — `FirebaseAuthService` is mocked via `@MockBean`. AuthController and security filters have ~48 unit tests. Postman tests cover end-to-end with real JWT tokens. |
| @WebMvcTest for controllers | LeaveRequestController, LeaveAllowanceController, StaffController, and AuthController are tested with @WebMvcTest + MockMvc + @WithMockUser. Tests verify HTTP mapping, status codes, JSON structure, and facade delegation. Postman collections provide end-to-end coverage with real JWT tokens. |
| Mockito for handlers/services | Isolates the class under test. Proves coordination logic without needing a database. |
| @DataJpaTest for integration | Proves the full service→domain→persistence pipeline works with real SQL without loading Firebase/RabbitMQ. |
| Test listeners separately (not via events) | Allowance listeners are BEFORE_COMMIT (synchronous). Unit tests prove delegation. Integration test proves atomic commit/rollback. |
| Object Mother over Test Data Builder | Simpler for our use case. Mother methods create valid objects; tests only override what they're testing. |

---

## 6. Coverage Summary

| Module | Layer | Tested By | Method |
|--------|-------|-----------|--------|
| Common | Domain (VOs, supertypes) | Unit tests | Plain JUnit |
| Common | EventStoreService | Unit tests | Mockito |
| Leave Mgmt | Domain (aggregates, VOs, events) | Unit tests | Plain JUnit |
| Leave Mgmt | Mappers (6 classes) | Unit tests | Plain JUnit |
| Leave Mgmt | Query Handlers | Unit tests | Mockito |
| Leave Mgmt | Application Services | Integration tests | @DataJpaTest |
| Leave Mgmt | Event Listeners (4) | Unit tests + Integration | Mockito (unit) + @DataJpaTest with real BEFORE_COMMIT listeners (integration) |
| Leave Mgmt | Controllers | Unit tests (@WebMvcTest + MockMvc) | @WithMockUser, JSON assertions |
| Staff Mgmt | Domain (aggregate) | Unit tests | Plain JUnit |
| Staff Mgmt | Mappers (3 classes) | Unit tests | Plain JUnit |
| Staff Mgmt | Query Handler | Unit tests | Mockito |
| Staff Mgmt | Application Service | Unit tests | Mockito |
| Staff Mgmt | Event Listeners (2) | Unit tests | Mockito |
| Staff Mgmt | Controllers | Unit tests (@WebMvcTest + MockMvc) | @WithMockUser, JSON assertions |
| Identity | Auth flow | Unit tests (@WebMvcTest + Mockito) | MockMvc + mocked FirebaseAuthService |

---

## 7. Running Tests

### All tests (unit + integration):
```bash
mvn test
```

### Unit tests only (exclude integration):
```bash
mvn test -Dtest="!com.staffs.leavebooking.integration.*"
```

### Integration tests only:
```bash
mvn test -Dtest="com.staffs.leavebooking.integration.*"
```

### Single test class:
```bash
mvn test -Dtest="com.staffs.leavebooking.leavemanagement.domain.LeaveRequestTest"
```

### From IntelliJ:
- Right-click `src/test/java` → Run All Tests
- Right-click any test class → Run
- Right-click a `@Nested` inner class → Run just that group

---

## 8. Lecture Alignment

| Lecture Concept | How We Apply It |
|-----------------|-----------------|
| Lecture 2: AAA pattern | Every test follows Arrange/Act/Assert |
| Lecture 2: Properties of good tests (FIRST) | Fast, Isolated, Repeatable, Self-validating, Thorough |
| Lecture 2: Object Mother | `LeaveRequestMother` and helper methods in test classes |
| Lecture 2: `@DisplayName` + `@Nested` | Every test class uses both |
| Lecture 2: Test VOs and Entities | All domain objects comprehensively tested |
| Lecture 7: Event flows | Integration tests prove service flows; unit tests prove listener delegation |
| Lecture 9: Postman testing | Comprehensive collection with JWT management (`pm.globals.set`) — `postman/` folder |
| K22: Unit testing as a development technique | 467 tests, 0 failures — across all architectural layers |

---

## 9. Test Counts

| Category (selected breakdown — approximate) | Count |
|----------|-------|
| Domain unit tests (common + leave + staff) | ~184 |
| Mapper unit tests | ~41 |
| Query handler unit tests | ~17 |
| Controller unit tests (@WebMvcTest) | ~18 |
| Event listener unit tests (local + remote consumers) | ~11 |
| Notification publisher/consumer unit tests | ~10 |
| Application service unit tests (LeaveRequest + LeaveAllowance) | ~25 |
| EventStoreService unit tests | ~6 |
| Security filter unit tests (RateLimitFilter, SecurityHeaders, UnauthorisedAccessLogger, FirebaseTokenFilter) | ~28 |
| Identity (AuthController + FirebaseAuthService) unit tests | ~48 |
| Event store cleanup job unit tests | ~6 |
| Integration tests (@DataJpaTest) | 23 |
| **Confirmed total (mvn clean verify)** | **467 run, 0 failures, 0 errors, 0 skipped** |
| Postman API tests | 139 requests across 8 folders |

*(Confirmed: 467 run, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS)*

---

## 10. JaCoCo Code Coverage

JaCoCo is configured in `pom.xml` (prepare-agent + report in test phase). After `mvn clean verify`, the report is at `target/site/jacoco/index.html`.

### Coverage Summary (as of 2026-08-31)

| Metric | Coverage |
|--------|----------|
| **Instruction coverage** | **78%** (1,522 of 7,171 missed) |
| **Branch coverage** | **61%** (179 of 468 missed) |
| **Line coverage** | **78%** (359 of 1,612 missed) |
| **Method coverage** | **78%** (92 of 409 missed) |
| **Class coverage** | **91%** (9 of 104 missed) |

### Coverage by Package

| Package | Instructions | Branches | Notes |
|---------|-------------|----------|-------|
| `leavemanagement.domain` | 99% | 99% | Near-perfect — core DDD domain logic fully tested |
| `staffmanagement.domain` | 99% | 100% | Near-perfect — aggregate, VOs, state machine |
| `common.domain` | 93% | 88% | Value objects, supertypes, guard clauses |
| `leavemanagement.application.listeners` | 94-100% | — | All 4 leave event listeners + notification publishers |
| `leavemanagement.application.handlers` | 88% | 72% | Query handlers and application services |
| `leavemanagement.application.mappers` | 94% | n/a | JPA ↔ domain ↔ DTO mappers |
| `staffmanagement.ui` | 73% | 52% | Controller layer (edge case paths reduce coverage) |
| `leavemanagement.ui` | 79% | 61% | Controller layer with ownership checks |
| `identity.security` | 73% | 66% | Security filters (RateLimit, Headers, Token) |
| `identity` | 57% | 36% | AuthController (Firebase mocked in tests) |
| `identity.authService` | 33% | 27% | **Lowest** — `FirebaseAuthService` calls real Firebase SDK which is mocked in tests. The actual HTTP calls to Firebase Identity Toolkit and Admin SDK cannot run without a live Firebase project. |

### Why Identity Coverage Is Low

The `identity.authService` package (33%) contains `FirebaseAuthService` which makes real HTTP calls to:
- Firebase Identity Toolkit REST API (`identitytoolkit.googleapis.com`) for login
- Firebase Admin SDK (`FirebaseAuth.getInstance().createUser()`) for registration
- Firebase Admin SDK for password changes and role updates

These are external cloud API calls that are not executed in the unit test environment — the service is mocked via `@MockBean` in `@WebMvcTest` controller tests. Mocking the Firebase SDK directly is possible (and `FirebaseAuthServiceTest` does mock `FirebaseAuth`), but the real HTTP calls to the Identity Toolkit REST API are tested via Postman against the live running application.

### Why Facade Packages Show 0%

The `com.staffs.leavebooking.leavemanagement` (0%) and `com.staffs.leavebooking.staffmanagement` (0%) packages each contain a single class — the facade (`LeaveManagementFacade`, `StaffManagementFacade`). These are thin delegation layers where every method is 1-2 lines: apply `@PreAuthorize`, delegate to a handler, return the result. They show 0% because:

- **`@WebMvcTest` controller tests** mock the facade entirely via `@MockBean` — the real facade code never executes.
- **Unit tests for handlers/services** test below the facade — they call the handler directly, not through the facade.
- **Postman tests** exercise the facades end-to-end against the live application, but JaCoCo only measures automated test coverage.

The real business logic lives in the handlers (88%) and domain (99-100%) which the facades delegate to. The facades themselves contain no logic beyond the `@PreAuthorize` annotation and the delegation call. Adding facade-level integration tests would bump the number but would only test that delegation works — not any additional business logic.

### How to View

```bash
mvn clean verify
# Then open in browser:
# target/site/jacoco/index.html
```
