# Requirements Traceability — Leave Booking System

**Module:** COMP60047 Enterprise Application Development
**Scenario:** Scenario 1 — Leave Booking System
**Last Updated:** 2026-08-26

This document maps every requirement from the assignment brief to its implementation and test coverage.

---

## 1. Staff Requirements

| # | Requirement | Implementation | Endpoint | Test Coverage |
|---|-------------|----------------|----------|---------------|
| S1 | Request leave (annual leave, subject to manager approval) | `LeaveRequest.submitNew()` → `LeaveRequestApplicationService.submitNewRequest()` | `POST /leave-requests` | Unit: `LeaveRequestTest$SubmitNew` (11 tests). Integration: `shouldPersistLeaveRequest`, `shouldRejectPastStartDate`, `shouldRejectEndBeforeStart`. Controller: `LeaveRequestControllerTest$SubmitRequest`. |
| S2 | Cancel a leave request (approved or pending) | `LeaveRequest.cancel()` → `LeaveRequestApplicationService.cancelRequest()` | `PATCH /leave-requests/{id}/cancel` | Unit: `LeaveRequestTest$Cancel` (9 tests). Integration: `shouldCancelPending`, `shouldCancelApproved`. Controller: `LeaveRequestControllerTest$CancelRequest`. |
| S3 | View status of leave requests (Pending, Approved, Rejected) | `LeaveRequestQueryHandler.findRequestsByStaffMemberId()` (unfiltered) and `searchByStaffMember()` (filtered by status via POST search) | `GET /leave-requests/my` (all requests), `POST /leave-requests/my/search` (filtered — e.g. `{"status": "PENDING"}`) | Unit: `LeaveRequestQueryHandlerTest$FindByStaffMemberId` (2 tests), `$SearchByStaffMember` (2 tests). Controller: `LeaveRequestControllerTest$GetMyRequests` (1 test), `$SearchMyRequests` (2 tests). |
| S4 | View remaining annual leave / days used | `LeaveAllowanceQueryHandler.findAllowanceByStaffMemberId()` | `GET /leave-allowances/my` | Unit: `LeaveAllowanceQueryHandlerTest$FindByStaffMemberId` (2 tests). Controller: `LeaveAllowanceControllerTest$GetMyAllowance` (1 test). Integration: `shouldAmendEntitlement`. |

---

## 2. Manager Requirements

| # | Requirement | Implementation | Endpoint | Test Coverage |
|---|-------------|----------------|----------|---------------|
| M1 | View outstanding requests for team | `LeaveRequestQueryHandler.findRequestsByManagerId()` (unfiltered) and `searchByManager()` (filtered by status and/or date range) | `GET /leave-requests/team` (unfiltered), `POST /leave-requests/team/search` (filtered — supports status, from, to). Brief enhancement "could be enhanced with start and end dates" is implemented via the POST search body. | Unit: `LeaveRequestQueryHandlerTest$FindByManagerId` (1 test), `$SearchByManager` (3 tests). Controller: `LeaveRequestControllerTest$GetTeamRequests` (1 test), `$SearchTeamRequests` (3 tests). |
| M2 | Approve a request | `LeaveRequest.approve()` → `LeaveRequestApplicationService.approveRequest()` | `PATCH /leave-requests/{id}/approve` | Unit: `LeaveRequestTest$Approve` (7 tests). Integration: `shouldApprove`. Controller: `LeaveRequestControllerTest$ApproveRequest` (1 test). Listener: `LeaveRequestApprovedListenerTest` (1 test). |
| M3 | Reject a request | `LeaveRequest.reject()` → `LeaveRequestApplicationService.rejectRequest()` | `PATCH /leave-requests/{id}/reject` | Unit: `LeaveRequestTest$Reject` (6 tests). Integration: `shouldReject`. Controller: `LeaveRequestControllerTest$RejectRequest` (1 test). Listener: `LeaveRequestRejectedListenerTest` (1 test). |
| M4 | View remaining leave for a staff member | `LeaveAllowanceQueryHandler.findAllowanceByStaffMemberId()` | `GET /leave-allowances/staff/{staffMemberId}` | Unit: `LeaveAllowanceQueryHandlerTest$FindByStaffMemberId` (2 tests). Controller: `LeaveAllowanceControllerTest$GetForStaff` (1 test). |

---

## 3. Admin Requirements

| # | Requirement | Implementation | Endpoint | Test Coverage |
|---|-------------|----------------|----------|---------------|
| A1 | Add a new member of staff | `StaffMember.createNew()` creates with PENDING_SETUP status. Admin activates via `PATCH /staff/{id}` with `{"employmentStatus":"ACTIVE"}` which triggers `StaffMemberAddedEvent` → RabbitMQ → creates `LeaveAllowance`. Self-registration also creates skeleton staff record. | `POST /staff` (admin), `POST /auth/register` (self-registration creates skeleton) | Unit: `StaffMemberTest$CreateNew` (11 tests), `$CreateSkeleton` (4 tests), `StaffApplicationServiceTest$AddNewStaffMember` (3 tests), `$CreateSkeleton` (2 tests). Controller: `StaffControllerTest$AddStaffMember` (1 test). |
| A2 | Amend role or department of staff | `StaffMember.updateDepartment()`, `StaffMember.updatePlacement()`, `StaffMember.updateStatus()` via unified `PATCH /staff/{id}` | `PATCH /staff/{id}` with any combination of: `department`, `lineManagerId`, `currentRole`, `startDateOfCurrentRole`, `jobLevel`, `employmentType`, `employmentStatus` | Unit: `StaffMemberTest$UpdateDepartment` (3 tests), `$UpdatePlacement` (4 tests), `$UpdateStatus` (9 tests), `StaffApplicationServiceTest$UpdateDepartment` (3 tests), `$UpdatePlacement` (3 tests), `$UpdateStatus` (3 tests). Controller: `StaffControllerTest$UpdateStaff` (5 tests). |
| A3 | View all outstanding leave requests (filtered by staff/manager/company) | `LeaveRequestQueryHandler.searchAll()` with `LeaveRequestSearchCriteria` supporting staffMemberId, managerId, status, and date range filters | `GET /leave-requests/all` (unfiltered), `POST /leave-requests/all/search` (filtered — status, staffMemberId OR managerId (mutually exclusive), from, to) | Unit: `LeaveRequestQueryHandlerTest$SearchAll` (6 tests). Controller: `LeaveRequestControllerTest$SearchAllRequests` (6 tests), `$GetAllRequests` (1 test). |
| A4 | Amend annual leave entitlement | `LeaveAllowance.amendEntitlement()` → `LeaveAllowanceApplicationService.amendEntitlement()` | `PATCH /leave-allowances/{id}/entitlement` | Unit: `LeaveAllowanceTest$AmendEntitlement` (5 tests). Integration: `shouldAmendEntitlement` (1 test). Controller: `LeaveAllowanceControllerTest$AmendEntitlement` (1 test). |
| A5 | Approve on behalf of managers | Same `approveRequest()` endpoint — ADMIN role has access via `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` | `PATCH /leave-requests/{id}/approve` | Covered by M2 tests above. RBAC enforced at facade. |

---

## 4. Architectural Requirements

| # | Requirement | Implementation | Evidence |
|---|-------------|----------------|----------|
| AR1 | Leave Management bounded context (core) | `com.staffs.leavebooking.leavemanagement` package with facade, domain, application, infrastructure, ui layers | `docs/05-folder-structure-design.md`, `LeaveManagementFacade.java` |
| AR2 | Staff Management bounded context (supporting) | `com.staffs.leavebooking.staffmanagement` package with same layered structure | `StaffManagementFacade.java` |
| AR3 | Identity & Access Control (non-DDD, generic) | `com.staffs.leavebooking.identity` package — no domain layer, direct service | `AuthController.java`, `FirebaseAuthService.java`, `SecurityConfig.java` |
| AR4 | Local events (within Leave Management) | `LeaveRequestSubmittedEvent`, `ApprovedEvent`, `RejectedEvent`, `CancelledEvent` → `@TransactionalEventListener` listeners → update `LeaveAllowance` | `docs/02-event-architecture-design.md`. Unit tests: 4 listener tests + `LeaveRequestTest` event raising. Integration: allowance operation tests. |
| AR5 | Remote events (Staff → Leave via RabbitMQ) | `StaffMemberAddedEvent`, `StaffMemberUpdatedEvent` → `EventStoreService` (outbox) → `RemoteOutboxListener` → RabbitMQ → `StaffMemberAddedListener`/`StaffMemberUpdatedListener` | `docs/02-event-architecture-design.md`. Unit: `EventStoreServiceTest`, listener tests. Integration: `shouldCreateAllowanceForNewStaff`. |
| AR6 | State machine (Pending → Approved/Rejected, Cancel from any) | `LeaveRequestStatus` enum + `LeaveRequest` aggregate guards transitions | Unit: `LeaveRequestTest` — approve/reject/cancel tests verify state guards (rejects wrong state). |
| AR7 | RBAC (Staff/Manager/Admin roles) | `Role` enum, `@PreAuthorize` on all facade methods, Firebase custom claims | `SecurityConfig.java`, `LeaveManagementFacade.java`, `StaffManagementFacade.java`. Unit: `AuthControllerTest$RoleCheck`. |
| AR8 | DDD patterns (entities, VOs, aggregates, repositories) | `LeaveRequest` (aggregate root), `LeaveAllowance` (aggregate root), `StaffMember` (aggregate root), `DateRange`/`BusinessYear`/`LeaveReason` (value objects), `Identity`/`Email`/`FullName` (common VOs) | `docs/01-domain-model-design.md`. Unit: all domain tests (~184 tests). |
| AR9 | CQRS (separate read/write paths) | Query handlers (read) vs Application services (write). Separate mappers for each direction. | `LeaveRequestQueryHandler`, `LeaveRequestApplicationService`. Unit: query handler tests + integration tests. |
| AR10 | Facade / Open Host Service | `LeaveManagementFacade`, `StaffManagementFacade` — single public entry points per bounded context | Controllers only talk to facades. |

---

## 5. Testing Requirements (from Marking Criteria)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Comprehensive automated unit testing | ✅ 321 unit tests | All domain objects, mappers, query handlers, app services, listeners, EventStoreService, controllers, identity |
| Follows best practice | ✅ | AAA pattern, @DisplayName, @Nested, Object Mother, FIRST properties — documented in `docs/07-testing-strategy-design.md` |
| Automated integration testing | ✅ 14 integration tests | `@DataJpaTest` + `@Import` — service→domain→repository→H2 |
| API testing (Postman) | ⬜ Task 14 pending | Collections to cover all endpoints with valid/invalid data per role |
| Coverage of all endpoints | Partially ✅ | Controller tests verify HTTP mapping. Postman will add full valid/invalid/role testing. |

---

## 6. Design Document Coverage (for Report)

| Report Section | Design Doc | Content |
|----------------|-----------|---------|
| Domain model (aggregates, VOs, invariants) | `docs/01-domain-model-design.md` | PlantUML class diagrams, field descriptions, state machines |
| Event architecture (local + remote) | `docs/02-event-architecture-design.md` | Event flow diagrams, outbox pattern, listener chain |
| Database schema (ERD, data dictionary) | `docs/03-database-schema-design.md` | ERD, table definitions, constraints, indexes |
| API endpoints (parameters, responses, errors) | `docs/04-api-endpoint-design.md` | All endpoints per role, curl examples, error responses |
| Folder structure (architecture, layers) | `docs/05-folder-structure-design.md` | Module tree, layer responsibilities, visibility rules |
| Issues and fixes | `docs/06-issues-and-fixes.md` | Every compilation/runtime issue and resolution |
| Testing strategy | `docs/07-testing-strategy-design.md` | Testing pyramid, patterns, coverage table, commands |
| Requirements traceability | `docs/08-requirements-traceability.md` | This file — maps every requirement to code + tests |

---

## 7. Summary

| Metric | Count |
|--------|-------|
| Staff requirements implemented | 4/4 (100%) |
| Manager requirements implemented | 4/4 (100%) |
| Admin requirements implemented | 5/5 (100%) |
| Architectural requirements | 10/10 (100%) |
| Total unit tests | 437 |
| Total integration tests | 14 |
| Total tests | 451 |
| Test classes | 44 |
| Design documents | 8 |
| Bounded contexts | 3 (Leave Management, Staff Management, Identity) |
| Domain events (local) | 4 |
| Domain events (remote) | 2 |
| API endpoints | ~20 |
| Remaining work | Task 14: Postman collections |
