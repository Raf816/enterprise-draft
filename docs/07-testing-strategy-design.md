# Testing Strategy Design — Leave Booking System

**Module:** COMP60047 Enterprise Application Development
**Assignment:** Scenario 1 — Leave Booking System
**Last Updated:** 2026-08-26

---

## 1. Testing Pyramid

```
           ┌─────────────┐
           │   API Tests  │  ← Postman collections (Task 14)
           │  (Manual/E2E)│     Full HTTP, JWT, role-based
           └──────┬───────┘
              ┌───┴────────────┐
              │ Integration    │  ← @DataJpaTest + @Import
              │ Tests (14)     │     Service → Domain → H2
              └──────┬─────────┘
         ┌───────────┴──────────────┐
         │    Unit Tests (239+)     │  ← Plain JUnit 5 + Mockito
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

The local event listeners use `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`. In `@DataJpaTest`, the test transaction never commits (it rolls back), so these listeners never fire. Instead we:
1. Test the **service methods** that listeners call (reserveDays, etc.) — proves the logic
2. Test the **listeners themselves** with Mockito — proves correct delegation
3. Together: complete proof that the event flow works

---

## 4. API Test Layer (Postman)

### 4.1 Purpose
Test the full HTTP request/response cycle including:
- Authentication (Firebase JWT tokens)
- Authorisation (role-based access control)
- Request validation
- Error responses (401, 403, 404, 400)
- End-to-end event flows (POST /staff → RabbitMQ → leave_allowance created)

### 4.2 Collections (Task 14 — Pending)
- Identity (register/login)
- Leave Requests (submit/approve/reject/cancel)
- Leave Allowances (view/amend)
- Staff Members (CRUD)

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
| No Identity module tests | Firebase is external — mocking the full Firebase SDK adds complexity with little value. Postman tests cover this layer. |
| @WebMvcTest for controllers | LeaveRequestController, LeaveAllowanceController, StaffController, and AuthController are tested with @WebMvcTest + MockMvc + @WithMockUser. Tests verify HTTP mapping, status codes, JSON structure, and facade delegation. Postman collections will add end-to-end coverage with real JWT tokens. |
| Mockito for handlers/services | Isolates the class under test. Proves coordination logic without needing a database. |
| @DataJpaTest for integration | Proves the full service→domain→persistence pipeline works with real SQL without loading Firebase/RabbitMQ. |
| Test listeners separately (not via events) | @TransactionalEventListener(AFTER_COMMIT) won't fire in test transactions. Direct invocation proves the delegation. |
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
| Leave Mgmt | Event Listeners (4) | Unit tests | Mockito |
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
mvn test -Dtest="com.staffs.leavebooking.integration.LeaveRequestIntegrationTest"
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
| K22: Unit testing as a development technique | 451 tests across all architectural layers |

---

## 9. Test Counts

| Category | Count |
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
| Integration tests (@DataJpaTest) | 14 |
| **Total** | **451** |
| Postman API tests | 138 requests across 8 folders |

*(Confirmed: 451 run, 0 failures, 0 errors, 1 skipped — BUILD SUCCESS)*
