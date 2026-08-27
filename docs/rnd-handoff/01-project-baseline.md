# Project Baseline

## Product Purpose

MINIS is a mobile-first platform for short, high-value vertical mini films and useful visual content. The PRD describes a broader MVP with a Flutter mobile app, guest feed browsing, creator upload, interactions, comments, reports, notifications, creator-centered chat, and a simple admin dashboard. The current repository implementation is narrower: it is an event-oriented React browser client plus a Spring Boot backend slice.

## Current Backend Architecture

The backend is a Java 21 Spring Boot modular monolith under `backend/minis-backend`, with package root `com.end.minisbackend`. Implemented feature packages include `identity`, `user`, `creator`, `admin`, `shared`, and an untracked `content` package in the current working tree. Package-by-feature boundaries are backed by ArchUnit tests in `backend/minis-backend/src/test/java/com/end/minisbackend/ArchitectureTest.java`.

PostgreSQL owns application data. Firebase proves external identity only. Spring Security verifies bearer Firebase ID tokens, maps the verified Firebase UID to an internal MINIS user UUID, and loads backend-owned roles and account status from PostgreSQL.

## React Event Client

The current client is `web/event-app`, a React/Vite/TypeScript app using Firebase Web SDK, React Router, and TanStack Query. It implements browser sign-up, sign-in, email verification, password reset, Google sign-in, backend bootstrap coordination, account projection, public creator routes, event-admin preparation screens, and an untracked event feed path.

## Flutter Status

`mobile/flutter/.gitkeep` exists, but no Flutter application source is present. The PRD and older architecture docs still describe Flutter as the primary MVP client. In this checkout, Flutter is `PLANNED`, not implemented.

## Current Development Phase

Committed `main` is at the Phase 3 authentication/creator/admin baseline. The working tree contains uncommitted Phase 4/event media read-path work. This handoff does not continue Phase 4 and does not start Phase 5.

## Current Event Scope Versus Broader Product Scope

The event scope is a browser/demo slice: Firebase auth, local backend account bootstrap, public creator discovery, event-admin creator preparation, curated imported media, signed MinIO read URLs, and a React guest feed. The broader product scope remains mobile-first and includes creator upload, comments, likes, saves, follows, reports, notifications, chat, analytics, search/discover, and mobile store release work. Most of that broader scope is planned or deferred rather than implemented.

## Capability Table

| Capability | Classification | Git state | Evidence |
|---|---|---|---|
| Modular monolith backend | IMPLEMENTED | Committed with modified tests | `backend/minis-backend/pom.xml`, `backend/minis-backend/src/test/java/com/end/minisbackend/ArchitectureTest.java`, `docs/adr/0002-modular-monolith-backend.md` |
| Firebase ID-token verification | IMPLEMENTED | Committed | `backend/minis-backend/src/main/java/com/end/minisbackend/identity/application/FirebaseTokenVerifier.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/identity/infrastructure/FirebaseBearerAuthenticationFilter.java` |
| Firebase browser auth flows | IMPLEMENTED | Committed with modified React routes | `web/event-app/src/core/auth/AuthSessionContext.tsx`, `web/event-app/src/core/auth/firebaseClient.ts` |
| Backend account bootstrap | IMPLEMENTED | Committed | `backend/minis-backend/src/main/java/com/end/minisbackend/identity/api/AuthBootstrapController.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/application/UserAccountBootstrapService.java` |
| PostgreSQL-owned role/status | IMPLEMENTED | Committed | `backend/minis-backend/src/main/java/com/end/minisbackend/user/domain/UserAccount.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/domain/UserRole.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/domain/AccountStatus.java`, `backend/minis-backend/src/main/resources/db/migration/V1__create_identity_user_accounts.sql` |
| Creator identity and admin preparation | IMPLEMENTED | Committed with modified query support | `backend/minis-backend/src/main/java/com/end/minisbackend/creator/`, `backend/minis-backend/src/main/java/com/end/minisbackend/admin/`, `backend/minis-backend/src/main/resources/db/migration/V2__create_creator_profiles_and_event_admin_audit.sql` |
| React event/admin preparation client | IMPLEMENTED | Committed with modifications | `web/event-app/src/app/App.tsx`, `web/event-app/src/features/admin/`, `web/event-app/src/features/creators/` |
| Curated media import and read path | IMPLEMENTED_NOT_VERIFIED | Untracked source and migration | `backend/minis-backend/src/main/java/com/end/minisbackend/content/`, `backend/minis-backend/src/main/resources/db/migration/V3__create_content_items.sql`, `docs/media-and-upload.md` |
| MinIO signed media read URLs | IMPLEMENTED_NOT_VERIFIED | Untracked content package; modified config | `backend/minis-backend/src/main/java/com/end/minisbackend/content/application/ContentStorage.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/infrastructure/MinioContentStorage.java`, `backend/minis-backend/src/main/resources/application.yml`, `compose.demo.yml` |
| React public feed | IMPLEMENTED_NOT_VERIFIED | Untracked feed feature; modified routes | `web/event-app/src/features/feed/`, `web/event-app/src/app/App.tsx` |
| Live creator upload | DEFERRED | Not implemented | `docs/media-and-upload.md`, `docs/architecture/decision-backlog.md` |
| Transcoding, thumbnails, media worker | DEFERRED | Not implemented | `docs/media-and-upload.md`, `docs/architecture/decision-backlog.md` |
| Comments, likes, saves, follows, reports | PLANNED | Not implemented | `docs/prd/minis-prd-v1.1.md`; absence of matching backend packages/controllers |
| Notifications and FCM outbox | PLANNED | Documentation only | `docs/adr/0001-backend-owned-notification-system.md`, `docs/designs/notification-system-technical-design.md` |
| Creator-centered chat | PLANNED | Documentation only | `docs/prd/minis-prd-v1.1.md`, `docs/architecture/decision-backlog.md` |
| Flutter app | PLANNED | Placeholder only | `mobile/flutter/.gitkeep` |
| Admin web app under `web/admin` | PLANNED | Placeholder only | `web/admin/.gitkeep` |
