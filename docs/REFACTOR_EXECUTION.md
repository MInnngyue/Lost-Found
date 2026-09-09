# Lost Found Refactor Execution

## Plan authority and continuation

Read this file at the beginning of every refactoring session. Do not infer completion
from conversation summaries, an HTTP 200 response, or a successful build.

The stage baseline is `F:\Users\kangc\Desktop\Lost-Found重构方案.docx`, read again on
2026-09-05. Later user-approved business decisions are in
`docs/BUSINESS_REQUIREMENTS_V1.md` and `docs/DATA_MODEL_V1.md`.
These decisions supersede two parts of the initial Word draft:

- Public credit scores, score-based restrictions and credit-score ledgers are removed.
  Implement reports, violation cases, explicit sanctions, restrictions and appeals.
- Classification and location data use `lf_category` with `ITEM`, `COLOR`, `CAMPUS`,
  `AREA`, `LOCATION`. The active baseline is business schema v2, not the v1 draft.

The user authorized continuous implementation through all stages without asking
permission to proceed after each stage. Pause only for a genuinely required manual
Navicat operation. Never request passwords or expose runtime secrets.
Do not overwrite the original Word file or the original project/database.

The files under `portal/docs/PROJECT_CONTEXT.md`, `portal/docs/WORK_STATE.md` and
the Hermes task references are inherited history from the old project. Their
Boot 3, old workspace paths and July task statuses are not current refactor facts.
The current repository root is `G:\VibeCodingFiles\Lost-Found`.

## Milestones and acceptance gates

| Stage | Scope | Evidence required | Current status |
| --- | --- | --- | --- |
| M0 | Preserve old project, root Git, baseline and migration map | Original code/data remain available; source and target identified | Complete for the current workspace layout; migration audit remains a documentation task |
| M1 | RuoYi, Spring Boot 4, MyBatis, admin, modular monolith | Builds and real startup | Complete locally: backend starts on `8081`, Spring Boot `4.1.0`, Java 21 |
| M2 | User migration, login, roles, menus, object permissions | Ordinary user, reviewer, operator and administrator access tests; legacy user mapping | Complete locally; four-role live acceptance passed on 2026-09-06 |
| M3 | Categories, fields, images, post lifecycle, review | Real user create/submit, admin view/review, square/detail visibility, edit/rejection/withdrawal/expiry, image persistence/privacy | Complete locally; live create/upload/approve/public/detail/image path and regression gates passed |
| M4 | Claims, matching and governance replacing credit scores | Transactional two-party completion, conflicts/duplicates/concurrency/permission tests, match score boundaries, sanctions | Complete in source and service acceptance; governance permissions and state guards passed |
| M5 | Complete independent portal migration | Claims, notifications, comments, private messages, reports, appeals, profile, statistics; no legacy API dependence | Complete locally; independent portal tests and production build passed |
| M6 | Migration, security, CI, deploy, backup/restore | Clean install; fixed dependencies; least privilege; upload/abuse tests; restore drill; deployment guide | Local engineering baseline complete; production perimeter and second-instance restore smoke test are deployment handoffs |

Do not begin the next business stage before the prior business workflow passes.
M2 gaps needed for M3 must be closed as part of its access-control acceptance.
Schema tables and old Vue pages are not implementations of their workflows.
Production WAF/DDoS protection and campus identity integration cannot be claimed
as deployed based on local code or local rate-limiter tests.

## Current checkpoint: M3/M4 status, governance and security convergence

Confirmed against the live backend on 2026-09-06:

- Category, claim/match, interaction and moderation seed scripts have been
  executed in the isolated `ry-vue-lostfound` database.
- Enabled ITEM/COLOR/CAMPUS roots and hierarchy prerequisites pass.
- Real post creation, image upload, review approval, public visibility and image
  access have passed a live acceptance path.
- Claim and match status vocabulary has been aligned in source code; the
  idempotent status migration was executed successfully in `ry-vue-lostfound`
  through Navicat, with the final status distribution queries completed.
- Portal no longer contains the obsolete custom administration UI or public
  credit-score operations.
- 举报对象已覆盖帖子、评论、用户、认领过程和私信；认领页与私信页提供
  对应入口，并由服务层校验举报人与目标对象的关系。

Final verification notes:

- Shared publish/edit required-field rules and category-loading state.
- Empty/failed classification prevents submission; retry is visible.
- Title includes its required marker; future/invalid dates are rejected.
- Backend rejects missing, disabled, wrong-type and cross-campus selections.
- Draft submission rechecks categories; rejection/removal requires a reason.
- Frontend submission holds a lock before asynchronous validation, and a saved
  draft with a failed submission returns to "My Posts" instead of creating another.
- Optional area can be cleared during editing instead of retaining an old campus's area.
- `backend/sql/lost_found_category_seed.sql` adds missing defaults without
  resetting user-created records. All sample locations are explicitly test locations.
- `scripts/Test-PostPrerequisites.ps1` exits nonzero while live required data is missing.

The source tests do not deploy changes into an already-running
`ruoyi-admin.jar`; the final live service checks below were run against the
rebuilt local service.

### Current source evidence

- portal tests: 12/12 passed.
- backend full Maven test: 45/45 passed, including domain validation, mapper SQL
  qualification and post service state/permission/category tests.
- portal build: passed.
- admin production build: passed.
- isolated MySQL 8 seed test: passed for idempotency, user-record preservation,
  disabled-record preservation, hierarchy and existing-campus preservation.
- Playwright browser regression: passed for empty/error categories, retry, required
  title, duplicate submission prevention, saved-draft recovery and mobile edit
  validation. Authenticated API calls were mocked, so it is not live acceptance.
- live prerequisite check: passed for the isolated database.
- Business audit writes, application-layer rate limits and restricted local CORS
  are implemented; production WAF/DDoS protection is still a deployment concern.
- The backend JAR was started with `druid,lostfound`; `/lostfound/category/tree`
  returned HTTP 200 and loaded the isolated database's seeded and user-created
  categories. MySQL and Redis connections were confirmed in the startup log.
- The role-matrix completion script was executed in `ry-vue-lostfound`.
- Live unauthenticated smoke checks passed: portal `5173`, admin `80`, public
  post/category endpoints were reachable; protected system and governance
  endpoints rejected unauthenticated requests with RuoYi code `401`.
- Latest live service check passed: admin `http://localhost/`, portal
  `http://localhost:5173/` and backend `http://localhost:8081/` all returned
  HTTP 200. The image endpoint `/lostfound/post/1/images/1` returned HTTP 200
  with `image/png`, a valid PNG signature and 81,131 bytes, confirming the
  post-image mapper fix works in the running JAR.
- Latest live category prerequisite check passed: 8 enabled ITEM roots,
  11 enabled COLOR roots and 1 enabled CAMPUS root.
- Latest backend Maven regression run passed on 2026-09-06: 51 tests,
  0 failures and 0 errors; the full `ruoyi-admin` reactor build succeeded.
- The backend test suite includes category hierarchy and governance state-machine
  guards: invalid parent types, disabled parents, hierarchy cycles, disabling
  categories with children and reopening an open case are rejected.
- A fresh backend test run on 2026-09-06 passed: 51 tests, 0 failures, 0 errors.
- A fresh portal test run on 2026-09-06 passed: 12 tests, 0 failures.
- The mocked browser fixture now covers the portal's unread-count and private
  message bootstrap requests, so the documented browser regression runs with
  the current portal.
- Portal production build and admin production build both passed on 2026-09-06.
- CI now runs Portal unit tests before the production build, and the final
  manual role/restore procedure is documented in `docs/ACCEPTANCE_RUNBOOK.md`.
- Local backend credentials are no longer stored in source configuration; the
  rebuilt JAR requires `LOSTFOUND_DB_PASSWORD` at startup. The previous process
  was intentionally stopped before rebuilding and must be restarted after the
  variable is supplied. `scripts/Start-LostFound.ps1` now provides the masked
  local startup path, and its PowerShell syntax check passed.
- The new project default profile is now `druid,lostfound`, reducing the risk
  of accidentally starting against the original `ry-vue` database.
- `backend/.env.example` documents the non-secret runtime variables without
  containing local credentials.
- Full ordinary-user/reviewer/operator role acceptance passed on 2026-09-06 with
  temporary accounts created through the admin API and removed after verification.
  Positive and negative API checks passed for all three non-admin roles.
- The common-role seed was tightened after live acceptance found that the base
  RuoYi SQL had granted broad system-menu permissions to `common`. The updated
  `backend/sql/lost_found_common_role_grant.sql` removes non-Lost-Found grants
  and restores only the portal permissions. The active database was corrected
  through the admin role API and re-verified.
- Backup/restore structure and upload-file verification passed. The remaining
  restore login/detail/image smoke test requires starting a second backend
  process against the restored database and is intentionally kept isolated from
  the active service.

### Restore drill evidence (2026-09-06)

- A new complete backup was created at
  `_data/backups/ry-vue-lostfound_full_2026-09-06-v2.sql`.
- The backup artifact is 145,687 bytes and contains all 21 expected system and
  Lost-Found tables, all four controlled roles, and one `lf_post_image` record.
- The restore-test database was imported from the v2 backup. Its first
  verification result selected `ry-vue-lostfound-restore-test-v2`, and its role
  verification result contained `admin`, `common`, `lostfound_operator` and
  `lostfound_reviewer`.
- The referenced upload file exists at the active profile path and was copied
  without modification to
  `_data/restore-tests/ry-vue-lostfound-restore-test-v2/uploadPath`; both copies
  are 81,131 bytes.
- The restore-test database structure, roles and upload-file pairing passed.
  Post-restore login, detail and image access require a second backend instance
  configured with the restore-test database password. This is intentionally
  isolated from the active service and is the only remaining local handoff.

## Required Navicat operation

Use only the local **`ry-vue-lostfound`** database, not the original `ry-vue`.
The required operation was completed on 2026-09-06. For a fresh environment, run
`backend/sql/lost_found_status_migration.sql` as a complete UTF-8 SQL file with
"stop on error" enabled. Take a backup first. The file uses a transaction, does
not modify users or passwords, and does not drop tables.

Its final queries must show the current claim and match status distributions.
If an error occurs, roll back the uncommitted transaction and do not continue
with service restart or business acceptance until the error is resolved.

## M3 business acceptance checklist

- [x] Live ITEM/COLOR/CAMPUS counts and location relationships pass.
- [x] Fresh ordinary user has publish/mine/edit/submit but no administration permissions.
- [x] Missing fields, arbitrary category IDs, wrong hierarchy and future dates are rejected.
- [x] Upload validates actual image bytes, size and count; ownership and private access hold.
- [x] A real user's text and image post survives refresh with image associations intact.
- [x] Draft and pending content are absent from public list/detail/image access.
- [x] Reviewer can inspect full content and images before acting.
- [x] Approval exposes content; rejection includes a reason visible to its owner.
- [x] Owner can edit/retry a rejected draft without accidental duplicate creation.
- [x] Other users cannot edit, withdraw, inspect private drafts or review that post.
- [x] Withdrawal/removal/expiry removes public access and records the event.
- [x] Concurrent or repeated review/submit produces one valid state transition.
- [x] Review notifications reach only the owner; audit history is verifiable.
- [x] Desktop and mobile UI, automated tests and both builds pass.

The checked items are backed by the live role/workflow acceptance and the
51-test backend, 12-test portal, browser regression and production-build runs
recorded above. The restore-test second-instance smoke check remains separate
because it requires a database credential and must not interrupt the active
service.

This repository is being rebuilt as a modular monolith based on RuoYi and Spring Boot 4.

## Current layout

- `backend`: RuoYi Spring Boot 4 server and administration backend.
- `admin`: RuoYi Vue 3 administration console.
- `portal`: existing Vue 3 campus user application retained during migration.
- `legacy-backend`: original Spring Boot implementation kept as a comparison and rollback reference.
- `_data`: local database dump, runtime files, and uploads; excluded from Git.

## Migration rule

The original application is treated as the functional baseline. Each business capability is migrated only after the corresponding baseline behavior is documented and tested. Authentication, role, menu, and audit behavior are provided by RuoYi. Lost-and-found workflows remain in the `ruoyi-lostfound` business module.

## Build targets

- Java 17 or newer.
- Spring Boot 4.1.x.
- MyBatis Spring Boot 4.x.
- MySQL 8.
- Node.js 20.19 or newer for the portal and administration console.

## First milestone

The first milestone is a clean RuoYi server and administration console that compile independently while the legacy portal remains available. Business migration starts only after this foundation passes the build and startup checks.
