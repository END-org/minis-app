# R&D Handoff: Read Me First

## Purpose

This pack gives a separate R&D employee enough current MINIS context to investigate production deployment options for a release expected in approximately two months. It is evidence-based and scoped to integration boundaries, deployment touchpoints, and provider-sensitive decisions.

This is not a complete architecture audit, not a cloud-provider recommendation, and not an implementation phase.

## Intended Audience

The intended reader is an R&D employee who will compare deployment, authentication, database, storage, upload, and mobile release options without needing to read the complete source tree first.

## Repository Snapshot

- Generation date: 2026-08-05, Asia/Amman.
- Branch inspected: `main`.
- Commit inspected: `e73333ff3b9c26ed756e56bdb2d031c070d4a983`.
- Latest commit summary: merge commit for `phase-3-complete-auth`, dated 2026-07-27.
- Git status summary: branch up to date with `origin/main`; no staged changes; many unstaged modifications exist; untracked Phase 4/media-read-path files exist.

Important uncommitted evidence:

- Modified tracked files include root/backend/frontend docs, backend Maven/config/security/test files, `compose.demo.yml`, and React event-app files.
- Untracked files include `backend/minis-backend/src/main/java/com/end/minisbackend/content/`, `backend/minis-backend/src/main/resources/db/migration/V3__create_content_items.sql`, content tests, `docs/adr/0004-event-media-read-path.md`, `docs/media-and-upload.md`, and `web/event-app/src/features/feed/`.
- Treat the media/feed path as local working-tree evidence, not committed release baseline.

## Limitations

- No external provider, pricing, quota, or service-limit research was performed.
- No production, staging, Firebase Console, MinIO console, cloud account, DNS, Android, Apple, or TestFlight checks were performed.
- No source code, tests, migrations, Docker files, or application configuration were changed.
- Backend full `verify` was not run because it is heavier than this handoff and uses the full Maven/Testcontainers quality gate.
- Frontend production build was not run to avoid producing build artifacts during a documentation-only task.
- Curated event media was not manually imported or played in browsers/devices.

## Classification Definitions

- `IMPLEMENTED`: Meaningful implementation exists.
- `IMPLEMENTED_NOT_VERIFIED`: Implementation exists but was not successfully verified during this analysis.
- `PARTIALLY_IMPLEMENTED`: Some meaningful pieces exist, but the complete workflow is unavailable.
- `PLANNED`: Documented but not meaningfully implemented.
- `DEFERRED`: Explicitly postponed.
- `LOCAL_OR_DEMO_ONLY`: Suitable only for local development, diagnostics, or the current demo topology.
- `UNKNOWN`: Not enough repository evidence.
- `DOCUMENTATION_CONFLICT`: Documents and implementation disagree.

## Commands Actually Executed

| Command | Result |
|---|---|
| `git branch --show-current` | `main` |
| `git rev-parse HEAD` | `e73333ff3b9c26ed756e56bdb2d031c070d4a983` |
| `git status` and `git status --short` | Branch up to date with `origin/main`; no staged changes; unstaged modifications and untracked Phase 4/media files present. |
| `git log -1 --decorate --stat` | Latest commit is merge commit `e73333f...`, `feat(event): complete creator identity, Firebase auth, event admin fl...`. |
| `rg --files`, `git ls-files`, `git diff --name-status` | Used to distinguish tracked, modified, and untracked evidence. |
| Targeted `Get-Content -Raw` reads | Required docs, config, code, Docker, CI, migrations, and client touchpoints were inspected. |
| Filename-only secret scan | Found `.env`, `.env.example`, `secrets/minis-671fc-firebase-adminsdk-fbsvc-95e5d4a30e.json`, `web/event-app/.env.example`, and `web/event-app/.env.local`. Contents were not read. |
| `git status --short --ignored -- ...` and `git check-ignore -v -- ...` | `.env`, `secrets/`, and `web/event-app/.env.local` are ignored; `.env.example` and `web/event-app/.env.example` are tracked templates. |
| `.\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests compile` | Backend compile succeeded; Maven reported `BUILD SUCCESS`. |
| `cmd /c npm.cmd run lint` | Frontend lint succeeded with 7 existing React Fast Refresh warnings and 0 errors. |
| `cmd /c npm.cmd run typecheck` | Frontend typecheck succeeded. |
| `cmd /c npm.cmd run test` | First sandboxed run failed with `spawn EPERM`; rerun outside sandbox succeeded: 6 test files passed, 43 tests passed. |

## Backend Owner Review And Approval

- [ ] Confirms classifications distinguish committed and uncommitted work correctly.
- [ ] Confirms no secrets or private identifiers are exposed beyond repository-relative credential-like paths.
- [ ] Confirms provider-sensitive change surfaces are accurate enough for R&D research.
- [ ] Confirms production-readiness gaps are not overstated as recommendations.
- [ ] Approves copying `docs/rnd-handoff/rnd-repo-seed/` into a separate R&D repository.
