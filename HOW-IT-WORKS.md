# How It Works — Leave Booking System

> A complete walkthrough of the system architecture, every layer, how data flows, and what each class does.
> Includes real code from the source files with explanations so you can explain every part during presentation.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [1. App Entry Point](#1-app-entry-point)
- [2. Bounded Contexts and Module Structure](#2-bounded-contexts-and-module-structure)
- [3. The Request Lifecycle — Complete Flow](#3-the-request-lifecycle--complete-flow)
- [4. Domain Layer — Business Rules](#4-domain-layer--business-rules)
- [5. Application Layer — CQRS Orchestration](#5-application-layer--cqrs-orchestration)
- [6. Infrastructure Layer — Persistence](#6-infrastructure-layer--persistence)
- [7. Event System — How Aggregates Communicate](#7-event-system--how-aggregates-communicate)
- [8. Identity and Security](#8-identity-and-security)
- [9. Data Transformations at Each Layer](#9-data-transformations-at-each-layer)
- [10. Testing Structure](#10-testing-structure)
- [11. Full Data Flow Example — Submit Leave Request](#11-full-data-flow-example--submit-leave-request)
- [12. Full Data Flow Example — Add Staff Member (Remote Event)](#12-full-data-flow-example--add-staff-member-remote-event)
- [13. Key Design Patterns](#13-key-design-patterns)
- [14. Tech Stack](#14-tech-stack)

---

## Architecture Overview

The system follows Evans'' layered architecture with CQRS separation, implemented as a Spring Modulith:

```
HTTP Request
     |
     v
[Controller] -----> thin HTTP adapter, extracts auth context
     |
     v
[Facade] ----------> public module API, @PreAuthorize RBAC
     |
     +--- READ PATH (Query) -------> [QueryHandler] --> Repository --> JPA Entity --> DTO Mapper --> JSON
     |
     +--- WRITE PATH (Command) ----> [ApplicationService] --> Repository --> JPA Entity
                                            |                                     |
                                            v                                     v
                                     [Domain Aggregate] <-- JpaToDomainMapper -- [Load]
                                            |
                                            v (validate + raise events)
                                     [DomainToJpaMapper] --> Repository.save()
                                            |
                                            v
                                     [DomainEventManager] --> EventStore + Spring EventPublisher
                                            |
                                            v (after commit)
                                     [Listener] --> updates other aggregates or publishes to RabbitMQ
```

**Key principles:**
- Domain layer has ZERO framework dependencies (pure Java)
- Aggregates are loaded from persistence, mutated via command methods, and saved back
- Events are raised by aggregates and dispatched AFTER the transaction commits
- Read path never touches domain aggregates (performance optimisation)

---

## 1. App Entry Point

### LeavebookingApplication.java

```java
@EnableRabbit       // Activates RabbitMQ listener containers
@EnableAsync        // Enables @Async on event listeners (separate threads)
@EnableRetry        // Enables @Retryable/@Recover on outbox publisher
@SpringBootApplication
public class LeavebookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeavebookingApplication.class, args);
    }
}
```

| Annotation | Purpose |
|---|---|
| `@SpringBootApplication` | Combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@EnableRabbit` | Activates `@RabbitListener` annotations for consuming remote events |
| `@EnableAsync` | Allows `@Async` event listeners to run on separate threads |
| `@EnableRetry` | Activates `@Retryable` for the outbox publisher retry mechanism |

### GlobalExceptionHandler.java

Centralised error handling via `@ControllerAdvice`. Catches exceptions thrown anywhere in the request pipeline and converts them to consistent JSON error responses:

- `IllegalArgumentException` -> 400 Bad Request (validation failures)
- `IllegalStateException` -> 409 Conflict (business rule violations like invalid state transitions)
- `*NotFoundException` -> 404 Not Found
- `FirebaseAuthException` -> 400 Bad Request (auth failures)
- `Exception` -> 500 Internal Server Error (unexpected)

---

## 2. Bounded Contexts and Module Structure

The system is decomposed into three bounded contexts:

| Context | Type | Package | DDD Applied? |
|---|---|---|---|
| Leave Management | Core | `leavemanagement/` | Yes (aggregates, VOs, events) |
| Staff Management | Supporting | `staffmanagement/` | Yes (aggregate, events) |
| Identity & Access | Generic | `identity/` | No (uses Firebase directly) |

Plus a shared kernel: `common/` (supertypes, event infrastructure).

Each DDD context follows the same internal structure:
```
contextroot/
+-- ContextFacade.java       (PUBLIC - Open Host Service)
+-- ui/                      (HIDDEN - controllers)
+-- application/             (HIDDEN - handlers, mappers, DTOs, commands, listeners)
+-- domain/                  (HIDDEN - aggregates, VOs, enums, events)
+-- infrastructure/          (HIDDEN - JPA entities, repositories)
```

Spring Modulith enforces that only the Facade is visible to other modules.

---

## 3. The Request Lifecycle -- Complete Flow

Every HTTP request passes through these layers in order:

### Inbound (Request):
1. **Tomcat** receives HTTP request
2. **Security Filter Chain** validates JWT (FirebaseTokenFilter + OAuth2 Resource Server)
3. **RateLimitFilter** checks rate limits (login endpoint only)
4. **SecurityHeadersFilter** adds/removes security headers
5. **Controller** parses request, extracts auth context
6. **Facade** checks @PreAuthorize role permissions
7. **Handler** (QueryHandler or ApplicationService) performs the operation

### Outbound (Response):
8. Handler returns result (DTO for queries, void for commands)
9. Controller wraps in ResponseEntity with status code
10. If exception thrown at any layer -> GlobalExceptionHandler catches and returns error JSON

---

## 4. Domain Layer -- Business Rules

The domain layer is pure Java with NO framework dependencies. It contains:

### Aggregates (consistency boundaries):
- `LeaveRequest` - state machine (PENDING -> APPROVED/REJECTED/CANCELLED)
- `LeaveAllowance` - balance tracking (entitlement, used, pending)
- `StaffMember` - employee record with terminal state invariant

### Value Objects (immutable, equality by value):
- `Identity<T>` - UUID wrapper with type safety
- `FullName` - validated first name + surname
- `Email` - regex-validated email address
- `DateRange` - start/end dates with working days calculation
- `BusinessYear` - start year + end year (always consecutive)
- `LeaveReason` - validated text (max 500 chars)

### Key domain rules enforced:
1. Leave requests cannot start in the past
2. Only PENDING requests can be approved/rejected
3. Only PENDING or APPROVED requests can be cancelled
4. daysUsed + daysPending + newRequest <= totalEntitlement (no overbooking)
5. TERMINATED staff cannot be reactivated
6. Hire date cannot be in the future

All rules are enforced via `DomainAssertions` utility methods that throw `IllegalArgumentException` or `IllegalStateException`.

---

## 5. Application Layer -- CQRS Orchestration

### Query Path (Read):
```
Controller -> Facade -> QueryHandler -> Repository.findBy...() -> JPA Entity -> JpaToDTOMapper -> DTO
```
- Never loads domain aggregates
- Maps JPA directly to DTO (fast, no domain logic needed for reads)
- No transactions needed (read-only)

### Command Path (Write):
```
Controller -> Facade -> ApplicationService -> Repository.findById() -> JPA Entity
    -> JpaToDomainMapper -> Domain Aggregate (validates + raises events)
    -> DomainToJpaMapper -> Repository.save()
    -> DomainEventManager.manageDomainEvents()
```
- Loads the aggregate from JPA via mapper
- Calls the command method on the aggregate (which validates invariants)
- Maps back to JPA and saves
- Dispatches events after save

### ApplicationService Pattern (every write method follows this):
```java
@Transactional
public void approveRequest(String leaveRequestId, String decidedBy) {
    LeaveRequest leaveRequest = loadDomainAggregate(leaveRequestId);
    leaveRequest.approve(decidedBy);

    leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.toJpa(leaveRequest));
    dispatchAndClear(leaveRequest);

    log.info("Leave request {} approved by {}", leaveRequestId, decidedBy);
}

/** Extracted helper — eliminates repetition across all command methods. */
private void dispatchAndClear(LeaveRequest aggregate) {
    if (aggregate.domainEventsExist()) {
        domainEventManager.manageDomainEvents(
                this.getClass().getSimpleName(),
                aggregate.listOfDomainEvents()
        );
        aggregate.clearDomainEvents();
    }
}
```

---

## 6. Infrastructure Layer -- Persistence

### JPA Entities
Separate from domain aggregates. Use Lombok (@Getter/@Setter) and Jakarta Validation annotations.
Fields stored as strings for enums (e.g. `"PENDING"`, `"ANNUAL"`, `"FULL_TIME"`).

### Data Mappers (6 in Leave Management, 3 in Staff Management)
Static utility classes with no state:
- `DomainToJpaMapper` - after aggregate mutation, converts back to JPA for saving
- `JpaToDomainMapper` - loads aggregate from JPA using `reconstitute()` factory (no events)
- `JpaToDTOMapper` - read path, JPA directly to DTO (no domain involved)

### Repositories
Spring Data `CrudRepository` interfaces with custom query methods:
```java
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestJpa, String> {
    List<LeaveRequestJpa> findByStaffMemberId(String staffMemberId);
    List<LeaveRequestJpa> findByManagerId(String managerId);
    List<LeaveRequestJpa> findByStatus(String status);
}
```

---

## 7. Event System -- How Aggregates Communicate

### Local Events (within Leave Management):
```
LeaveRequest.approve() -> raises LeaveRequestApprovedEvent
    -> DomainEventManager saves to event_store (status=LOCAL) + publishes to Spring
    -> [TRANSACTION COMMITS]
    -> LeaveRequestApprovedListener (@Async, @TransactionalEventListener(AFTER_COMMIT))
        -> LeaveAllowanceApplicationService.confirmDays(staffMemberId, numberOfDays)
            -> LeaveAllowance.confirmDays() (daysPending -= n, daysUsed += n)
```

### Remote Events (Staff Management -> Leave Management via RabbitMQ):
```
StaffMember.createNew() -> raises StaffMemberAddedEvent
    -> DomainEventManager saves to event_store (status=PENDING) + publishes to Spring
    -> [TRANSACTION COMMITS]
    -> RemoteOutboxListener (@Async, @TransactionalEventListener(AFTER_COMMIT), @Retryable)
        -> RabbitTemplate.convertAndSend("staff-management", "staff.member.added", event)
        -> event_store status -> PUBLISHED
    -> [NETWORK - RabbitMQ broker]
    -> StaffMemberAddedListener (@RabbitListener)
        -> LeaveAllowanceApplicationService.createAllowanceForNewStaff(...)
            -> Creates new LeaveAllowance (25 days, 0 used, 0 pending)
```

### Notification Events (Leave Management -> RabbitMQ -> Notification Consumers):
```
LeaveRequest.submitNew() -> raises LeaveRequestSubmittedEvent (local)
    -> ManagerNotificationPublisher (@TransactionalEventListener)
        -> raises ManagerNotificationEvent (remote)
        -> RemoteOutboxListener publishes to "leave-notifications" / "notification.manager.pending"
    -> [NETWORK - RabbitMQ broker]
    -> ManagerNotificationConsumer (@RabbitListener) -> logs manager alert

LeaveRequest.approve()/reject() -> raises Approved/RejectedEvent (local)
    -> StaffNotificationPublisher (@TransactionalEventListener)
        -> raises StaffNotificationEvent (remote)
        -> RemoteOutboxListener publishes to "leave-notifications" / "notification.staff.decided"
    -> [NETWORK - RabbitMQ broker]
    -> StaffNotificationConsumer (@RabbitListener) -> logs staff alert
```

### RabbitMQ Infrastructure (Auto-Provisioned):
All exchanges, queues, and bindings are declared as Spring beans in `RabbitInfrastructureConfig.java`.
Spring AMQP auto-creates them on the broker at startup — no manual setup required.

| Exchange | Queue | Routing Key |
|---|---|---|
| `staff-management` | `leave-management.staff-member-added` | `staff.member.added` |
| `staff-management` | `leave-management.staff-member-updated` | `staff.member.updated` |
| `leave-notifications` | `notifications.manager-pending-request` | `notification.manager.pending` |
| `leave-notifications` | `notifications.staff-request-decided` | `notification.staff.decided` |

### Why AFTER_COMMIT?
If the listener fires DURING the transaction and fails, it would roll back the producers data (the leave request was valid - it should not fail because of a listener issue). AFTER_COMMIT ensures the producers data is safely persisted before any side effects run.

---

## 8. Identity and Security

### Registration Flow:
```
POST /auth/register {username, email, password, role}
    -> AuthController.register()
    -> FirebaseAuthService.registerUser()
        -> firebase.createUser(CreateRequest)  [creates account in Firebase]
        -> firebase.setCustomUserClaims({role: "ADMIN"})  [embeds role in future JWTs]
    <- UserRecord (uid, email, displayName)
    <- 201 Created {uid, email, username, message}
```

### Login Flow:
```
POST /auth/login {emailOrUsername, password}
    -> AuthController.login()
    -> FirebaseAuthService.loginUser()
        -> POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword
    <- LoginResponse {uid, email, idToken (JWT), refreshToken, expiresIn}
```

### Authenticated Request Flow:
```
GET /leave-requests/all  [Authorization: Bearer <JWT>]
    -> FirebaseTokenFilter: verifyIdToken(token), extract role, set SecurityContext
    -> OAuth2 Resource Server: validate JWT signature against Google JWKS
    -> AuthorizationFilter: check authenticated
    -> Controller -> Facade: @PreAuthorize("hasRole(''ADMIN'')") <- checked here
    -> Handler executes
    <- Response
```

### Security Features:
- Rate limiting (20 req/min on /auth/login per IP) via Bucket4j
- Server header obfuscation (removes Server, X-Powered-By; adds HSTS, nosniff, DENY)
- Unauthorised access logging (logs all 401/403 with IP, user, endpoint, reason)
- Windows-ROOT SSL trust for corporate SSL inspection compatibility

---

## 9. Data Transformations at Each Layer

```
[HTTP Request Body]         -> SubmitLeaveRequestCommand (record)
[Command]                   -> LeaveRequest.submitNew() (Domain Aggregate)
[Domain Aggregate]          -> LeaveRequestDomainToJpaMapper.toJpa() -> LeaveRequestJpa
[LeaveRequestJpa]           -> Repository.save() -> H2 Database

[H2 Database]               -> Repository.findBy...() -> LeaveRequestJpa
[LeaveRequestJpa]           -> LeaveRequestJpaToDTOMapper.toDTO() -> LeaveRequestDTO (record)
[LeaveRequestDTO]           -> JSON response body
```

Each transformation has a dedicated mapper class. No magic - explicit, testable conversions.

---

## 10. Testing Structure

### Unit Tests (335 tests, 0 failures, < 3 seconds)
```
src/test/java/
+-- testfixtures/           Object Mother classes (pre-configured test data)
+-- common/domain/          Identity, FullName, Email, DomainAssertions tests
+-- leavemanagement/
|   +-- domain/             DateRange, BusinessYear, LeaveReason, LeaveRequest, LeaveAllowance tests
|   +-- application/mappers/  All 6 mapper tests
+-- staffmanagement/
    +-- domain/             StaffMember tests
    +-- application/mappers/  All 3 mapper tests
```

### Testing Patterns Used:
- **AAA** (Arrange, Act, Assert) - every test method
- **Object Mother** - `LeaveRequestMother`, `LeaveAllowanceMother`, `StaffMemberMother`, `JpaEntityMother`
- **@DisplayName** - human-readable test names
- **@Nested** - groups tests by behaviour
- **No Spring context** - pure Java domain tests, millisecond execution

---

## 11. Full Data Flow Example -- Submit Leave Request

```
1. Staff member sends: POST /leave-requests
   Body: {startDate: "2026-09-15", endDate: "2026-09-19", leaveType: "ANNUAL", reason: "Holiday"}
   Header: Authorization: Bearer <JWT with role=STAFF, uid=staff-001>

2. FirebaseTokenFilter extracts uid + role from JWT, sets SecurityContext

3. LeaveRequestController.submitRequest()
   - Extracts staffMemberId from Authentication principal
   - Builds SubmitLeaveRequestCommand record

4. LeaveManagementFacade.submitLeaveRequest(command)
   - @PreAuthorize("hasAnyRole(''STAFF'', ''MANAGER'', ''ADMIN'')") passes

5. LeaveRequestApplicationService.submitNewRequest(command) [@Transactional]
   a. Identity.generateId() -> new UUID
   b. LeaveRequest.submitNew(id, staffId, mgrId, ANNUAL, dateRange, reason)
      - dateRange.validateFutureStart() -> passes (2026-09-15 is in future)
      - dateRange.workingDays() -> 5 (Mon-Fri)
      - Status set to PENDING
      - Raises LeaveRequestSubmittedEvent(leaveRequestId, staffMemberId, 5)
   c. LeaveRequestDomainToJpaMapper.toJpa(leaveRequest) -> LeaveRequestJpa
   d. leaveRequestRepository.save(jpa) -> INSERT INTO leave_request
   e. domainEventManager.manageDomainEvents(events)
      - EventStoreService.append(event) -> INSERT INTO event_store (status=LOCAL)
      - ApplicationEventPublisher.publishEvent(event.withId(42))
   f. leaveRequest.clearDomainEvents()

6. [TRANSACTION COMMITS] -> HTTP 201 returned to client

7. LeaveRequestSubmittedListener fires (separate thread, after commit)
   - leaveAllowanceService.reserveDays("staff-001", 5)
   - Loads LeaveAllowance for staff-001
   - leaveAllowance.reserveDays(5)
     - Checks: daysUsed(5) + daysPending(3) + 5 = 13 <= 25 -> OK
     - daysPending becomes 8
   - Saves updated LeaveAllowanceJpa
```

---

## 12. Full Data Flow Example -- Add Staff Member (Remote Event)

```
1. Admin sends: POST /staff
   Body: {firstName: "Alex", surname: "Johnson", email: "alex@co.com", ...}
   Header: Authorization: Bearer <JWT with role=ADMIN>

2. StaffController -> StaffManagementFacade -> StaffApplicationService.addNewStaffMember()
   a. StaffMember.createNew(...) validates:
      - hireDate not in future
      - All required fields not null/blank
      - Sets status = ACTIVE
      - Raises StaffMemberAddedEvent(staffId, firstName, surname, email, mgrId, dept, 25)
   b. StaffMemberDomainToJpaMapper.toJpa() -> save to staff_member table
   c. DomainEventManager:
      - EventStoreService.append(event) -> status=PENDING
      - ApplicationEventPublisher.publishEvent(event.withId(7))
   d. clearDomainEvents()

3. [TRANSACTION COMMITS] -> HTTP 201 returned to admin

4. RemoteOutboxListener fires (separate thread, after commit, @Retryable)
   - RabbitOutboxRouter.resolve(event) -> {exchange: "staff-management", routingKey: "staff.member.added"}
   - RabbitTemplate.convertAndSend("staff-management", "staff.member.added", event)
   - EventStoreService.updateStatus(7, PUBLISHED)

5. [RabbitMQ delivers message to leave-management.staff-member-added queue]

6. StaffMemberAddedListener (@RabbitListener) receives event
   - LeaveAllowanceApplicationService.createAllowanceForNewStaff(event)
   - LeaveAllowance.createNew(id, staffId, mgrId, "Alex", "Johnson", "Engineering", 25)
   - Saves to leave_allowance table (entitlement=25, used=0, pending=0)

7. Alex now has a leave allowance and can submit requests
```

---

## 13. Key Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Bounded Context | Top-level packages | Logical business boundaries |
| Aggregate Root | LeaveRequest, LeaveAllowance, StaffMember | Consistency boundaries |
| Value Object | Identity, FullName, Email, DateRange, etc. | Immutable, self-validating |
| CQRS | QueryHandler vs ApplicationService | Read/write separation |
| Factory Method | submitNew() vs reconstitute() | Event-raising vs read-path creation |
| Domain Event | 4 local + 4 remote events (8 total) | Inter-aggregate communication |
| Outbox Pattern | RemoteOutboxListener + event_store | Reliable cross-context publishing |
| Open Host Service | Facade at module root | Public API for inter-module comms |
| Data Mapper | 9 mapper classes | Domain <-> JPA <-> DTO conversion |
| Repository | Spring Data CrudRepository | Collection-like persistence |
| Observer | @TransactionalEventListener / @RabbitListener | Loose coupling |

---

## 14. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.4.1 | Application framework |
| Spring Modulith | 1.3.1 | Module boundary enforcement |
| Spring Data JPA | (via Boot) | Persistence |
| H2 Database | (via Boot) | In-memory DB |
| Spring Security + OAuth2 RS | (via Boot) | JWT authentication and RBAC |
| Firebase Admin SDK | 9.7.0 | Cloud user management |
| Spring AMQP | (via Boot) | RabbitMQ integration |
| Lombok | 1.18.42 | Boilerplate reduction |
| Bucket4j | 8.10.1 | Rate limiting |
| Spring Retry | (via Boot) | Outbox retry mechanism |
| JUnit 5 + Surefire | (via Boot) | Unit testing |