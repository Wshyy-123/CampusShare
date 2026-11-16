# Sports and place attendance integration verification

Date: 2026-11-16. Source: feature/XIA-7-XIA-8 at c1bed5b23a31c4054e71b2bf848cae3df8932f47; main baseline: 2cd69de4b77e8ba1159d5d158a40b269b73f8591. Contributor commits remain ancestors of the corrected integration. PO explicitly authorized correction and merge.

## Reproduced defects and corrections

- Original branch accepted checkout before end_at, releasing a checked-in sports booking early. Corrected service rejects early checkout; automatic completion and explicit checkout race under the resource/reservation locks.
- Original branch accepted a short booking's check-in after its end but before start+15m. Check-in is now restricted to [start_at, min(start_at+15m, end_at)).
- Original HTTP endpoints accepted attendance without scanned place evidence. The integrated endpoint requires a validated string resourceId plus source=QR, matches the locked reservation's resource, and records the server Session actor/time/source.
- Added concrete place URL/PNG generation, protected login return, read-only GET resolution and visible-page POST attendance for classroom/sports. Repeated scans never toggle checkout. Demo does not simulate a successful real attendance operation.
- Added classroom/sports 15m no-show and checked-in normal-end scheduled handling, per-row transactions and retry. Classroom no-show adds no invented violation; normal completion does not fabricate check_out_at. Sports checkout after automatic completion records checkout once without a second completion event.

## Executed locally

- Java 21 / Maven verify: 139 tests, 0 failures, 0 errors, 3 skipped (the existing MySQL-only tests). The author's 9 sports integration tests now run through the shared integration base instead of being silently gated by MYSQL_TEST.
- Regression coverage includes HTTP Session/CSRF, missing/wrong QR place, ownership, disabled/blocked resources, exact deadlines and short bookings, idempotency, classroom/no-show/end handling, no fake checkout, rollback/retry, 105-row pagination, concurrent check-in/no-show and automatic completion/manual checkout, and a real enabled Spring scheduler test.
- npm ci: 86 audited packages, no reported vulnerabilities. check:baseline, check:api (including new attendance checks), vue-tsc and production build passed. Frontend adapter tests use mocked HTTP/SSR; those results are distinct from the actual HTTP exercise below.
- Actual running Spring Boot + isolated H2 + Vite proxy: logged in using Session/CSRF, GET read-only, missing/wrong place rejected, repeat scan idempotent, early checkout rejected, classroom check-in, scheduler-driven classroom/sports no-show and normal completion, explicit post-completion checkout, slot rebooking/cancellation, foreign-account 404 and anonymous 401 all passed.
- Generated actual PNG for /places/201; decoded it using OpenCV and confirmed the exact location URL. Vite served the landing URL. The local test fixture and results are under the workspace .artifacts/sports_integration_2026-11-16; they are not production seed data.

## GitHub verification and remaining limits

The integration PR must pass the repository's MySQL 8.4 backend/migration and frontend workflows before merge. The PR checks and merge commit provide the final remote evidence; the local H2 run is not represented as a MySQL run. Migrations 001 through 007 remain unchanged and no new database migration is needed.

No independent browser click-through, visual screenshot acceptance, or real-phone camera scan was executed. Static location QR is not independent proof of physical presence. Real-phone validation requires a phone-reachable VITE_PUBLIC_BASE_URL and server; localhost points to the phone itself. Library simulated card entry/exit and classroom proactive cancellation remain outside this integration and incomplete.
