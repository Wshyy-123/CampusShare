# XIA-10 and XIA-13 verification

Date: 2026-11-16. Baseline dcf02871d595831ae137c9a992dc51d3a1e8eb48 (PR #12). PO authorized finishing the two remaining stories and confirmed classroom cancellation strictly before start, never after check-in. Library departure immediately releasing the seat was already confirmed on 2026-11-04.

## Automated tests

- Java 21 Maven verify: 163 tests, 0 failures/errors, 3 pre-existing MySQL-only tests skipped locally on H2. New LibraryClassroomIntegrationTest: 24 scenarios, all passing, no conditional test gating. GitHub MySQL 8.4 CI must pass before merge; run logs and PR checks provide the final MySQL evidence.
- New tests cover exact accepted_at+30m and one millisecond before, inconsistent stored deadline, ended/future bookings, ownership and wrong seats/types, Session/CSRF/source validation, removed insecure routes, disabled/blocked seats, active cancellation rejection for seats, separate idempotent entry/exit, early/late library departure, no-show protection, concurrent duplicate events and no-show race, event rollback/retry, classroom cancellation boundaries/roles/reasons/other occupancy, and actual booking creation after release.
- Frontend check-library exercises CARD_SIMULATION contract and CSRF, opaque BIGINT identifiers, actual API service cache/list refresh after library exit and classroom cancellation, visible separate controls using SSR, demo write rejection, and stale account response isolation. Existing baseline/API/type/build checks are required.

## Actual browser and HTTP exercise

Started actual Spring Boot with isolated in-memory H2 and an explicit test seed outside the repository, enabled the real seat and venue scheduler, and connected Vite API proxy. Used installed Python Playwright/headless Chromium with the real page and actual Session/CSRF/backend requests (no mocked fetch or intercepted responses).

- Protected booking URL redirects to login and returns to the booking after login; GET does not mark arrival. Seat has no active cancel button.
- Clicked simulated card entry, checked server checkInAt, repeated entry via HTTP, wrong seat rejection, clicked departure and confirmed COMPLETED and actual checkOutAt before reserved end. Repeated departure preserved the timestamp.
- Clicked classroom pre-start cancellation and confirmed USER_CANCEL plus updated bookings. Another real Session account successfully created overlapping bookings in the freed seat and classroom. Supplied userId did not allow acting for the owner.
- Actual scheduler cancelled the expired seat and absent classroom and completed the checked-in ended classroom without fake checkout.
- Desktop 1440x1000 and mobile-width 390x844 screenshots inspected; mobile had no horizontal overflow. No page script errors. These are browser viewport checks, not a physical-phone camera test.

The first browser exercise found success text was cleared when the parent replaced the reservation DTO: the child watcher returned a fresh array. Changed it to watch individual ID/account sources, repeated the complete browser flow and passed. This was a real UI correction rather than a claimed first-pass success.

Workspace evidence: .artifacts/library_classroom_2026-11-16/backend-focused.log, backend-final.log, browser-check.py, browser-result.json, library-desktop.png, library-mobile.png and classroom-desktop.png. Temporary servers were stopped after validation. No user MySQL database was touched; migrations 001-007 remain unchanged.

## Acceptance boundary

All 14 original stories now have core implementation paths; this is not a record of 44 acceptance criteria manually signed off or PingCode status changes. Physical phone QR/camera validation and full multi-role manual acceptance remain separate. Library card interaction is deliberately simulated, with no school card reader or teaching-system integration claim. The contributor history for earlier XIA-9/10 remains preserved; old unsafe runtime entry points were replaced, not re-enabled.
