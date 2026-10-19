# Appointment Closure Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a backward-compatible schedule-slot model and enforce one consistent appointment lifecycle with basic authenticated, CSRF-protected mutations.

**Architecture:** Preserve the Servlet/MyBatis architecture and current `appointments` identity. Add `schedule_slots` as the authoritative mutable schedule representation, migrate existing `dates` records, and encapsulate transitions in `AppointmentsServiceImpl`. Add narrow filters/helpers so new appointment routes derive identity from the session and reject unauthenticated, cross-role, or missing-CSRF requests.

**Tech Stack:** Java 8, Servlet 3.1, MyBatis 3.2.2, MySQL 8, JUnit 4, jQuery/HTML.

**Spec:** `docs/specs/2026-10-14-appointment-closure-phase-1.md`

## Global Constraints

- Preserve legacy `dates` and existing appointment primary keys; use additive MySQL 8 migrations.
- Do not change the current no-refund cancellation policy in this phase.
- Identity and authorization come only from `HttpSession`, never URL or form identity values.
- Write endpoints use POST and require a CSRF token; use 401, 403, 409, and 400 consistently.
- Use JUnit tests first and observe a focused test fail before production code is written.

## Review Focus

- A slot that is closed versus booked must not become reservable after a cancellation; test this in Task 2.
- Simultaneous requests for the same slot must result in exactly one successful booking; cover with MySQL integration fixture in Task 4.
- A cross-role session must not read or mutate another role's appointment; cover in Task 5 filter and servlet tests.
- A prior changed appointment must remain visible after rescheduling; cover in Task 3 state transition tests.
- Existing records without `schedule_slot_id` must be migrated and listable; cover migration preflight/backfill verification in Task 1.

---

### Task 1: Add additive appointment-slot migration and MyBatis model

**Files:**
- Create: `database/migrations/20261014_appointment_slots_and_states.sql`
- Create: `src/main/java/com/ustb/hospital/entity/ScheduleSlot.java`
- Create: `src/main/java/com/ustb/hospital/mapper/ScheduleSlotsMapper.java`
- Create: `src/main/resources/com/ustb/hospital/mapper/ScheduleSlotsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/entity/Appointments.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/AppointmentsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/AppointmentsMapper.xml`
- Modify: `src/main/resources/mybatis-config.xml`
- Test: `src/test/java/com/ustb/hospital/database/AppointmentSlotMigrationContractTest.java`

**Interfaces:**
- Produces `ScheduleSlotsMapper.reserveAvailableSlot(int doctorId, String date, String slot): int` and `releaseBookedSlot(long scheduleSlotId): int`.
- Produces appointment fields `scheduleSlotId`, `previousAppointmentId`, `startedAt`, `completedAt`, `cancelledAt`, `updatedAt`, `version`.

- [ ] **Step 1: Write the failing migration contract test**

Assert the migration text creates `schedule_slots`, has the doctor/date/slot unique key, backfills `dates`, and supports `RESCHEDULED` and `IN_PROGRESS` states.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `mvn -Dtest=AppointmentSlotMigrationContractTest test`
Expected: FAIL because the migration file and model are absent.

- [ ] **Step 3: Add the additive migration and mapper contracts**

Use an auto-increment slot primary key, explicit check constraints, indexed foreign keys, and `UPDATE ... WHERE slot_status='AVAILABLE'` reservation semantics. Add the MyBatis mapper to the configuration.

- [ ] **Step 4: Run the focused test and full suite**

Run: `mvn -Dtest=AppointmentSlotMigrationContractTest test` then `mvn test`
Expected: PASS.

### Task 2: Define appointment lifecycle transitions in the service layer

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/AppointmentStatus.java`
- Modify: `src/main/java/com/ustb/hospital/service/AppointmentsServiceImpl.java`
- Modify: `src/main/java/com/ustb/hospital/service/AppointmentOperationResult.java`
- Test: `src/test/java/com/ustb/hospital/service/AppointmentStatusTest.java`
- Test: `src/test/java/com/ustb/hospital/service/AppointmentLifecycleContractTest.java`

**Interfaces:**
- Produces `AppointmentStatus.canCancel(String)`, `canReschedule(String)`, `canStart(String)`, and `keepsSlotOccupied(String)`.
- Produces service methods `startAppointment(int doctorId, int appointmentId)` and a reschedule method that retains original history.

- [ ] **Step 1: Write failing status tests**

Assert only `BOOKED` can cancel, reschedule, or start; assert `CANCELLED` and `RESCHEDULED` release a slot, while `BOOKED`, `IN_PROGRESS`, and `COMPLETED` retain it.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `mvn -Dtest=AppointmentStatusTest,AppointmentLifecycleContractTest test`
Expected: FAIL because `AppointmentStatus` and new behavior do not exist.

- [ ] **Step 3: Implement status constants and transactional transitions**

Book reserves a new `ScheduleSlot`; cancel changes only owned `BOOKED` records and releases its slot; reschedule reserves the target, marks source `RESCHEDULED`, inserts a successor, then releases source; start validates doctor ownership and changes `BOOKED` to `IN_PROGRESS`.

- [ ] **Step 4: Run focused tests and full suite**

Run: `mvn -Dtest=AppointmentStatusTest,AppointmentLifecycleContractTest test` then `mvn test`
Expected: PASS.

### Task 3: Replace servlet status mutations with lifecycle endpoints

**Files:**
- Create: `src/main/java/com/ustb/hospital/servlet/AppointmentStartServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentBookingServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentsEditServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentsUpdateStatusServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentCompletionServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentServletSupport.java`
- Test: `src/test/java/com/ustb/hospital/servlet/AppointmentLifecycleServletContractTest.java`

**Interfaces:**
- Consumes the service transitions from Task 2.
- Produces `POST /appointments/start`; all lifecycle errors use `AppointmentServletSupport.writeResult` with 401/403/409/400 mapping.

- [ ] **Step 1: Write failing servlet contract tests**

Assert the start mapping exists, mutation servlets expose only `doPost`, and ownership comes from `SessionIdentity` instead of request identity fields.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `mvn -Dtest=AppointmentLifecycleServletContractTest test`
Expected: FAIL because `/appointments/start` and status response mapping are absent.

- [ ] **Step 3: Implement start endpoint and consistent response semantics**

Parse only resource identifiers and schedule choices from requests. Use the session-derived patient or doctor identity. Map unauthenticated, forbidden, conflict, and validation outcomes to standard HTTP codes.

- [ ] **Step 4: Run focused tests and full suite**

Run: `mvn -Dtest=AppointmentLifecycleServletContractTest test` then `mvn test`
Expected: PASS.

### Task 4: Add deterministic MySQL integration coverage for slot contention

**Files:**
- Create: `src/test/java/com/ustb/hospital/integration/AppointmentSlotConcurrencyIT.java`
- Create: `src/test/resources/appointment-integration.properties.example`
- Modify: `pom.xml`

**Interfaces:**
- Consumes `AppointmentsServiceImpl.bookAppointment` and a separately configured disposable MySQL schema.
- Produces an opt-in Maven integration-test profile; normal `mvn test` remains database-free.

- [ ] **Step 1: Write a failing opt-in integration test**

Using two threads and a barrier, submit two bookings for the same doctor/date/slot; assert one result succeeds, one reports conflict, and one slot/appointment remains active.

- [ ] **Step 2: Run the test against the documented empty integration database**

Run: `mvn -Pintegration -Dtest=AppointmentSlotConcurrencyIT test`
Expected: FAIL before the integration profile and fixture are implemented.

- [ ] **Step 3: Add an opt-in test profile and fixture instructions**

Read database URL, user, and password only from environment variables. Do not target the deployed database. Reuse the migration scripts to prepare and clear the fixture schema.

- [ ] **Step 4: Run focused integration test and normal suite**

Run: `mvn -Pintegration -Dtest=AppointmentSlotConcurrencyIT test` and `mvn test`
Expected: PASS when test database is configured; normal suite PASS without it.

### Task 5: Add session authorization and CSRF protection for appointment writes

**Files:**
- Create: `src/main/java/com/ustb/hospital/filter/AuthenticationFilter.java`
- Create: `src/main/java/com/ustb/hospital/filter/CsrfFilter.java`
- Create: `src/main/java/com/ustb/hospital/security/CsrfTokenService.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/LoginServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentServletSupport.java`
- Modify: `src/main/webapp/doctor-schedule/appointments.html`
- Modify: `src/main/webapp/hospro/appointment-records.html`
- Test: `src/test/java/com/ustb/hospital/filter/AuthenticationFilterTest.java`
- Test: `src/test/java/com/ustb/hospital/filter/CsrfFilterTest.java`

**Interfaces:**
- Produces `CsrfTokenService.issue(HttpSession): String` and `verify(HttpServletRequest): boolean`.
- Filters `/appointments/*` and schedule mutation routes; public availability remains explicitly allowed.

- [ ] **Step 1: Write failing filter tests**

Assert missing session returns 401, incompatible role returns 403, missing or wrong `X-CSRF-Token` returns 403, and a token stored in the same session succeeds.

- [ ] **Step 2: Run focused tests to verify failure**

Run: `mvn -Dtest=AuthenticationFilterTest,CsrfFilterTest test`
Expected: FAIL because filters and token service are absent.

- [ ] **Step 3: Implement filters and browser token propagation**

On successful login rotate session ID, issue a cryptographically random token, and add it to same-origin AJAX write requests. Do not authorize any request from page query parameters.

- [ ] **Step 4: Run focused tests and full suite**

Run: `mvn -Dtest=AuthenticationFilterTest,CsrfFilterTest test` then `mvn test`
Expected: PASS.

### Task 6: Align patient and doctor appointment pages with the state machine

**Files:**
- Modify: `src/main/webapp/doctor-schedule/appointments.html`
- Modify: `src/main/webapp/hospro/appointment-records.html`
- Modify: `src/main/webapp/doctor-schedule/date.html`
- Test: `src/test/java/com/ustb/hospital/web/AppointmentLifecyclePageContractTest.java`

**Interfaces:**
- Consumes lifecycle endpoints from Task 3 and CSRF contract from Task 5.
- Produces role-appropriate buttons for `BOOKED`, `IN_PROGRESS`, `RESCHEDULED`, `CANCELLED`, and `COMPLETED`.

- [ ] **Step 1: Write failing page contract tests**

Assert doctor page uses `/appointments/start` for booked appointments, patient page does not offer cancellation or reschedule outside `BOOKED`, and AJAX mutations send `X-CSRF-Token`.

- [ ] **Step 2: Run focused test to verify failure**

Run: `mvn -Dtest=AppointmentLifecyclePageContractTest test`
Expected: FAIL because existing pages complete booked appointments directly and do not send CSRF tokens.

- [ ] **Step 3: Implement state-driven page actions**

Render text through escaped DOM APIs, give failed requests their returned business message, refresh records after a mutation, and retain context-path-safe URLs.

- [ ] **Step 4: Run focused tests and the full suite**

Run: `mvn -Dtest=AppointmentLifecyclePageContractTest test` then `mvn test`
Expected: PASS.

### Task 7: Build the WAR and perform migration dry-run checks

**Files:**
- Modify: `README.md` or create `docs/operations/appointment-closure-phase-1.md`
- Test: migration preflight SQL in `database/migrations/20261014_appointment_slots_and_states.sql`

**Interfaces:**
- Consumes migration and all application changes from Tasks 1–6.
- Produces `target/ustbhospital.war` and documented backup, preflight, migration, deployment, and rollback commands.

- [ ] **Step 1: Document the expected deployment validation**

Specify backup, duplicate checks, migration application to a copy of the database, WAR replacement, Tomcat restart, and browser acceptance flow.

- [ ] **Step 2: Run the full build**

Run: `mvn clean package`
Expected: `BUILD SUCCESS` and `target/ustbhospital.war` exists.

- [ ] **Step 3: Record results without deploying**

Report the build result and explicitly leave production deployment to a separate, user-approved step.
