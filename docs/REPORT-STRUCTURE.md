# Assignment 1 Report — Leave Booking System

**Module:** COMP60047 Enterprise Application Development
**Student:** Raf Ahmed
**Scenario:** Scenario 1 — Leave Booking System
**Word Count:** [minimum 1500 words]

---

## 1. Introduction

<!-- Brief paragraph: chosen scenario, what the system does, tech stack (Spring Boot 3.4.1, Java 21, Spring Modulith 1.3.1, H2, Firebase, RabbitMQ). Keep short — 100-150 words. -->

---

## 2. Design Decisions (25 marks)

### 2.1 Scenario and Role Analysis

<!-- Table: For each role (Staff, Manager, Admin), list every action from the brief.
     Then: explicitly identify any OMITTED, ADDITIONAL or ALTERED actions.
     Mark scheme says: "Explicitly identify any omitted, additional or altered actions and justify each change."

     OMISSIONS to justify:
     - No HR role (admin acts as HR — brief scopes for single developer)
     - No two-stage approval (manager then HR)
     - No PUT/DELETE endpoints (justify: soft-delete, PATCH for partial updates)
     - No pagination (prototype scale)
     - No annual leave rollover (data model supports it, not implemented)
     - No full event sourcing aggregate replay (outbox pattern instead)

     ADDITIONS to justify:
     - Self-registration with PENDING_SETUP state
     - POST search endpoints (not just GET query params)
     - Date overlap detection
     - OutboxRecoveryJob (resilience poller)
     - Extra security (RateLimitFilter, SecurityHeadersFilter, UnauthorisedAccessLogger)
     - Ownership check on GET /leave-requests/{id}
-->

### 2.2 Bounded Contexts

<!-- Identify and explain:
     - Leave Management (CORE) — LeaveRequest, LeaveAllowance aggregates
     - Staff Management (SUPPORTING) — StaffMember aggregate
     - Identity & Access Control (GENERIC, non-DDD) — Firebase, no aggregates

     Mark scheme says: "Explain bounded contexts as core, supporting/subdomain and generic contexts such as Identity."

     Prior learning: reference Lecture 3 (aggregates), Lecture 4 (modulith, shared kernel)
-->

### 2.3 Architecture Comparison

<!-- Compare and justify:
     - Monolith vs Modulith vs Microservices → chose Modulith (why)
     - CQRS vs Service Layer → chose CQRS (why)
     - Event sourcing vs Outbox pattern → chose Outbox (why)
     - Direct communication via facade vs other approaches

     Mark scheme says: "Compare architectural/responsibility options"

     Prior learning: reference COMP50051 (SOLID, SRP influenced CQRS split)
-->

### 2.4 Folder Structure

<!-- State and justify the folder structure.
     Reference docs/05-folder-structure-design.md for the full tree.
     Explain Evans' 4-layer architecture per bounded context.
     Explain why Identity is flat (no domain layer).

     Include a folder structure diagram (can reference or embed from docs/05).
-->

### 2.5 Design Patterns

<!-- Table: pattern name, where used, why chosen, any rejected alternatives.

     REQUIRED patterns to discuss (from guidance):
     Strategic: Bounded Context, Ubiquitous Language, Shared Kernel, Event Aggregator, Outbox
     Tactical: Entity, Value Object, Aggregate/Aggregate Root, DTO, Domain Event
     Other: Factory, Controller, Singleton, Repository, DAO, Facade/OHS, Anti-Corruption Layer

     Mark scheme says: "Identify and justify selected design patterns and evaluate at least some plausible patterns that were rejected."

     Prior learning: reference COMP50051 (GoF patterns, Factory, Singleton, Strategy)
-->

### 2.6 Class Diagram

<!-- Include a class diagram showing structural separation.
     Focus on the core context (Leave Management).
     Mark scheme says: "If large, split it for the report or host it as a web-based diagram/image."

     Can generate from PlantUML in docs/01.
-->

### 2.7 Sequence Diagrams

<!-- Include 2-3 sequence diagrams. Mark scheme says: "not required for every interaction."
     Suggested:
     1. Submit leave request (POST → Controller → Facade → AppService → Domain → Events → Allowance update)
     2. Staff activation (PATCH → StaffMember → StaffMemberAddedEvent → RabbitMQ → LeaveAllowance creation)
     3. Approve request (PATCH → Controller → verify manager → Domain → Events → confirmDays)

     Reference docs/02 for existing PlantUML diagrams.
-->

### 2.8 ERD and Data Dictionary

<!-- Include the ERD diagram.
     Include the data dictionary table for each entity/table.
     Reference docs/03-database-schema-design.md.

     Mark scheme says: "Record field/attribute name, type, primary or foreign key status, indexes, constraints and a description explaining why the field exists."
-->

### 2.9 API Endpoints

<!-- List core-context endpoints with parameters, returned format, and error messages.
     Reference docs/04-api-endpoint-design.md.

     Mark scheme says: "List the core-context endpoints with parameters, returned format and error messages."
-->

### 2.10 Message Queues and Event Architecture

<!-- Identify proposed message queues, explain why cross-context communication needs them.
     State the information in each message. Explain subscriber use.

     Queues:
     - leave-management.staff-member-added (StaffMemberAddedEvent)
     - leave-management.staff-member-updated (StaffMemberUpdatedEvent)
     - leave-management.manager-notification (ManagerNotificationEvent)
     - leave-management.staff-notification (StaffNotificationEvent)

     Explain the outbox lifecycle:
     PENDING → @Retryable → PUBLISHED/FAILED → OutboxRecoveryJob → PUBLISHED

     Mark scheme says: "The guidance associates this criterion with implementing event sourcing."
     Be honest: we implement the event store + outbox pattern, not full aggregate replay.

     Prior learning: reference Lecture 7 (local events), Lecture 8 (remote events, outbox)
-->

### 2.11 Security Design Impact

<!-- Explain the design impact of security on the architecture.
     - Firebase as external auth provider (why not build our own)
     - JWT-based stateless auth
     - @PreAuthorize at facade level (not controller level — why)
     - RBAC: STAFF, MANAGER, ADMIN roles

     Mark scheme says: "Explain the design impact of security, error handling, logging and event handling."
-->

### 2.12 Error Handling and Logging Design Impact

<!-- Centralised GlobalExceptionHandler
     Custom exceptions (LeaveRequestNotFoundException, etc.)
     UnauthorisedAccessLogger for 401/403 audit
     Structured JSON error responses
-->

---

## 3. Implementation Decisions (45 marks)

### 3.1 Folder Structure Implementation

<!-- Diagram of actual folder structure.
     Explain how it communicates core, supporting, and generic bounded contexts.
     Show the module visibility and controlled dependencies (be honest about cross-module deps).

     Mark scheme says: "include a diagram of the overall folder structure and explain how it communicates the core, supporting/subdomain and generic bounded contexts."
-->

### 3.2 Mappings, Validation, and Error Handling

<!-- Provide examples of:
     - JPA ↔ Domain ↔ DTO mappings (Data Mapper pattern)
     - Validation (@NotBlank, @Size, domain guard clauses via DomainAssertions)
     - Centralised error handling (GlobalExceptionHandler)
     - Custom errors (LeaveRequestNotFoundException → 404)
     - Entity-to-DTO conversion
     - Commands and Requests (CQRS command records)

     Mark scheme says: "Provide examples of mappings, validation, centralised error handling, custom errors, entity-to-DTO conversion, and Commands and Requests where CQRS is used."

     Prior learning: reference COMP50051 (validation patterns, error handling from REST API lectures)
-->

### 3.3 Security Implementation

<!-- Explain:
     - FirebaseConfig (3 beans: FirebaseApp, FirebaseAuth, JwtDecoder)
     - SecurityConfig (filter chain, permitAll on /auth/**, authenticated on everything else)
     - FirebaseTokenFilter (OncePerRequestFilter, verifyIdToken)
     - FirebaseJwtAuthenticationConverter (custom claims → GrantedAuthority)
     - @PreAuthorize annotations on facade methods
     - RateLimitFilter (20 req/min, Bucket4j) — EXTRA security
     - SecurityHeadersFilter (HSTS, server fingerprint removal) — EXTRA security
     - UnauthorisedAccessLogger (401/403 audit logging) — EXTRA security

     Mark scheme says: "what intended security was present or absent and any extra security introduced"

     Prior learning: reference COMP50051 (Helmet headers, rate limiting, JWT from OOP module)
-->

### 3.4 CRUD Handling

<!-- Explain create, read, update and delete for the core domain.
     - CREATE: POST /leave-requests (submit), POST /staff (admin creates)
     - READ: GET endpoints (my, team, all, by-id, search)
     - UPDATE: PATCH /leave-requests/{id}/approve|reject|cancel, PATCH /staff/{id}, PATCH /leave-allowances/{id}
     - DELETE: Not exposed as API — justify soft-delete approach (TERMINATED, CANCELLED)
     - EventStoreCleanupJob: only actual deletion (infrastructure, not business data)

     Mark scheme says: "The guidance notes that indiscriminate deletion is not necessarily appropriate."

     Prior learning: reference UK Working Time Regulations, audit trail requirements
-->

### 3.5 Event Implementation

<!-- Explain local and remote events.
     Local: LeaveRequestSubmitted/Approved/Rejected/Cancelled → @TransactionalEventListener(BEFORE_COMMIT) → allowance updates
     Remote: StaffMemberAdded/Updated → EventStore → RemoteOutboxListener → RabbitMQ → consumers
     Notification: ManagerNotification/StaffNotification → RabbitMQ → consumer logging

     Explain event record contents and rationale.
     Explain RabbitMQ as the selected broker and its effect on code structure.
     Explain OutboxRecoveryJob as resilience enhancement.

     Mark scheme says: "Explain implemented local and remote events, event record contents and their rationale, the selected broker and the broker's effect on code structure."
-->

### 3.6 Architecture Statement

<!-- Mark scheme says:
     "State whether the solution is a monolith, modulith, microservice or another structure"
     "State whether CQRS was used and explain why and how it affected the architecture"
     "State whether event sourcing was used and explain how it affected the architecture"

     Be clear: Modulith with CQRS. Event store + outbox pattern, not full aggregate replay.
-->

---

## 4. Testing Decisions (30 marks)

### 4.1 Testing Strategy Overview

<!-- Testing pyramid diagram.
     476 total tests (453 unit + 23 integration), 0 failures, 0 skipped.
     141 Postman API tests across 8 folders.

     Mark scheme says: "Testing does not require 100% code coverage, but it must be comprehensive."
-->

### 4.2 Unit Testing

<!-- Explain:
     - What is tested (domain, mappers, handlers, listeners, controllers, security)
     - Patterns used (AAA, Object Mother, @Nested + @DisplayName, Mockito)
     - verify() philosophy (Khorikov Pillar 2 + Phil's lecture guidance)
     - Properties of good tests (FIRST)

     Provide code examples.

     Prior learning: reference COMP50051 (AAA pattern, Object Mother, London School mocking, Jest → JUnit parallel)
-->

### 4.3 Integration Testing

<!-- Explain:
     - @DataJpaTest + @Import approach (why not @SpringBootTest)
     - What integration tests prove (submit, approve, reject, cancel, allowance ops, date overlap, atomic consistency)
     - Event listener consideration (BEFORE_COMMIT + @DataJpaTest rollback)

     Prior learning: reference Lecture 2 (test pyramid), Lecture 7 (event flows)
-->

### 4.4 API Testing (Postman)

<!-- Explain:
     - Collection structure (8 domain-based folders)
     - Token management (Login scripts capture JWT + UID)
     - Newman CLI for automated runs
     - Edge case coverage (401, 403, 404, 400, 409)
     - Role-based testing (each endpoint tested per role)

     Run collection screenshots or Newman output.
-->

### 4.5 Code Coverage

<!-- JaCoCo coverage summary.
     Explain why identity module is low (Firebase mocked).
     Explain why facade packages show 0% (mocked via @MockBean).
     Show that domain is 99-100%.

     Mark scheme says: "Include coverage evidence and a rationale for the selected unit and integration tests."
-->

---

## 5. Prior Learning Reflection

<!-- This appears 5 times in the mark scheme: "Where possible we will see reflection on how previous learning has influenced the decision making here."

     Connect decisions to:
     - COMP50051 (OOP): SOLID, GRASP, GoF patterns, Jest testing → JUnit parallel, AAA pattern, Object Mother, REST API design, security headers, rate limiting, JWT
     - COMP60044 (Mobile Apps): Firebase Auth, MVVM → CQRS parallel, Hilt DI → Spring DI, Repository pattern
     - COMP50045 (Agile/Web): React testing → behaviour-based testing, WCAG awareness, JWT from web security

     Can be woven into each section OR collected here as a standalone section.
-->

---

## 6. Conclusion

<!-- Brief summary: what was built, key architectural decisions, test coverage, acknowledged simplifications.
     100-150 words. -->

---

## Appendices (if needed)

- A: Full folder structure tree (reference docs/05)
- B: Full ERD (reference docs/03)
- C: Full endpoint list (reference docs/04)
- D: Newman test run output
- E: JaCoCo coverage report screenshot
