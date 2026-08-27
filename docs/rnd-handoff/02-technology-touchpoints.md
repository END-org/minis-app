# Technology Touchpoints

Only provider-sensitive integration boundaries are covered here.

## A. Authentication

Firebase Authentication is the current external identity provider. The React event client uses Firebase Web SDK for email/password registration, email verification, password reset, email/password sign-in, and Google sign-in. The backend verifies Firebase ID tokens with Firebase Admin SDK through the narrow `FirebaseTokenVerifier` interface.

`/api/v1/auth/bootstrap` explicitly creates or loads the local MINIS account. The backend uses an internal UUID for product ownership. Firebase UID is a unique external identity, not the domain primary key. PostgreSQL owns `VIEWER`, `CREATOR`, `ADMIN`, `ACTIVE`, `SUSPENDED`, and `DISABLED`.

| Field | Current state |
|---|---|
| Current owner | Firebase owns external identity; Spring Boot/PostgreSQL own MINIS authorization. |
| Integration point | React Firebase Web SDK; backend Firebase Admin SDK; bearer ID token in `Authorization`; `POST /api/v1/auth/bootstrap`; `GET /api/v1/me`. |
| Data stored | Firebase UID, email projection, email verification flag, display name/avatar projection, internal UUID, role, account status, profile data. |
| Change surface | `web/event-app/src/core/auth/`, `web/event-app/src/features/authentication/`, `web/event-app/src/features/account/BackendSessionContext.tsx`, `backend/minis-backend/src/main/java/com/end/minisbackend/identity/`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/`, `backend/minis-backend/src/main/resources/application*.yml`, `compose.demo.yml`, frontend/backend env templates and tests. |
| Migration sensitivity | HIGH |
| Repository evidence | `docs/adr/0003-firebase-authentication-integration.md`, `docs/authentication.md`, `backend/minis-backend/src/main/java/com/end/minisbackend/identity/application/FirebaseTokenVerifier.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/identity/infrastructure/FirebaseAdminTokenVerifier.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/identity/infrastructure/FirebaseBearerAuthenticationFilter.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/application/UserAccountBootstrapService.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/user/domain/UserAccount.java`, `web/event-app/src/core/auth/AuthSessionContext.tsx`, `web/event-app/src/core/api/apiClient.ts`. |

Stable contracts to preserve:

- backend-controlled roles and status;
- internal MINIS UUIDs in business data;
- explicit bootstrap semantics;
- bearer-token authentication boundary or a carefully versioned replacement;
- standard JSON API error contract and `X-Correlation-ID`.

## B. PostgreSQL

PostgreSQL is the application database. Flyway migrations are the executable schema source of truth, and Hibernate validates the migrated schema. The committed schema includes identity, creator profile, and event-admin audit tables. The current working tree adds untracked content metadata migration `backend/minis-backend/src/main/resources/db/migration/V3__create_content_items.sql`.

| Field | Current state |
|---|---|
| Current owner | Spring Boot owns schema migrations and application writes; PostgreSQL owns durable business data. |
| Integration point | Spring datasource config, Flyway, JPA entities, Spring Data repositories, JDBC query adapters. |
| Data stored | `user_accounts`, `creator_profiles`, `event_admin_audit_entries`, and uncommitted `content_items`. Planned docs also assign notifications, chat, reports, and future media job state to PostgreSQL. |
| Change surface | `backend/minis-backend/src/main/resources/db/migration/`, `backend/minis-backend/src/main/resources/application.yml`, `backend/minis-backend/src/main/resources/application-local.yml`, `backend/minis-backend/src/main/resources/application-demo.yml`, `backend/minis-backend/src/main/resources/application-prod.yml`, `compose.local.yml`, `compose.demo.yml`, JPA entities and repositories under feature packages, Testcontainers tests. |
| Migration sensitivity | HIGH |
| Repository evidence | `backend/minis-backend/src/main/resources/application.yml`, `backend/minis-backend/src/main/resources/application-local.yml`, `backend/minis-backend/src/main/resources/application-demo.yml`, `backend/minis-backend/src/main/resources/application-prod.yml`, `backend/minis-backend/src/main/resources/db/migration/V1__create_identity_user_accounts.sql`, `backend/minis-backend/src/main/resources/db/migration/V2__create_creator_profiles_and_event_admin_audit.sql`, untracked `backend/minis-backend/src/main/resources/db/migration/V3__create_content_items.sql`, `backend/minis-backend/src/test/java/com/end/minisbackend/shared/infrastructure/DatabaseMigrationIT.java`. |

Hosting migration sensitivity:

- JDBC URL, username, password, SSL mode, pool limits, and migration permissions need environment decisions.
- Flyway must continue to run against the intended schema; `ddl-auto=validate` must remain.
- Existing local/demo volume policy is not a production backup or restore strategy.

## C. Object Storage And Media

The current storage integration is MinIO through a narrow `ContentStorage` port. The current read path is for curated local/demo event media, not live creator uploads.

Object keys are generated server-side for imports as `content/imports/{sha256(importSourceKey)}.mp4`. Raw object keys, import source keys, internal hostnames, and MinIO credentials are not exposed in public API responses. Signed read URLs are generated at response time and expire by configuration.

`ffprobe` is used for inspection only. It validates the bounded event MP4/H.264/AAC vertical profile and does not transcode, remux, rotate, repair, or thumbnail media.

| Field | Current state |
|---|---|
| Current owner | Spring Boot content application owns media metadata and signing; MinIO owns binary objects; PostgreSQL owns metadata. |
| Integration point | `ContentStorage` interface, `MinioContentStorage` adapter, `ContentStorageProperties`, importer runner, public feed/content/creator-content APIs. |
| Data stored | MinIO stores imported MP4 objects; PostgreSQL stores content metadata, creator ownership, content status, media content type, size, duration, object key, import source key, and removal fields. |
| Change surface | `backend/minis-backend/src/main/java/com/end/minisbackend/content/application/ContentStorage.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/infrastructure/MinioContentStorage.java`, content importer/reconciliation, content API response mappers, React feed media handling, storage env variables, Docker Compose, NGINX/demo routing. |
| Migration sensitivity | HIGH |
| Repository evidence | `docs/media-and-upload.md`, `docs/adr/0004-event-media-read-path.md`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/application/ContentStorage.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/infrastructure/MinioContentStorage.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/infrastructure/FfprobeMediaProbe.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/application/CuratedContentImportService.java`, `backend/minis-backend/src/main/java/com/end/minisbackend/content/application/PublicContentQueryService.java`, `web/event-app/src/features/feed/feedApi.ts`, `web/event-app/src/features/feed/FeedVideo.tsx`, `compose.demo.yml`. |

Missing or deferred:

- live creator upload;
- presigned upload sessions;
- resumable or multipart mobile upload;
- thumbnails;
- transcoding;
- media-processing worker;
- CDN integration;
- production bucket provisioning, lifecycle, backup, and restore.
