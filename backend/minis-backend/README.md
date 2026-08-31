# MINIS Backend

Spring Boot modular monolith backend for the MINIS mobile-first mini-film platform.

## Current Baseline

The current backend slice verifies Firebase ID tokens, rejects unverified password-provider identities before bootstrap, bootstraps a local MINIS user, exposes `/api/v1/auth/bootstrap` and `/api/v1/me`, supports creator profiles plus minimal event-admin preparation APIs, imports curated event media, stores media objects in MinIO, exposes public feed/content/category APIs with signed read URLs, supports ADMIN-only content removal, supports backend likes/saves/creator follows plus private liked/saved collections and authenticated viewer social state, returns the shared JSON API error contract, propagates `X-Correlation-ID`, denies local diagnostics/dev routes outside the `local` and `demo` profiles, and creates the schema through Flyway.

Product features such as live media upload, transcoding, thumbnails, comments, reports, moderation queues, notifications, analytics, chat, frontend social UI, and complete user management are not implemented in this baseline.

## Prerequisites

- JDK 21 target. The local build can run on newer JDKs when Maven compiles with the Java 21 release target.
- Maven Wrapper 3.3.4, downloading Apache Maven 3.9.16 when needed.
- Docker and Docker Compose.
- Firebase development project.
- Firebase Authentication Email/Password provider enabled.
- Firebase Authentication Google provider enabled.
- Firebase Web App registered.
- Firebase Admin service-account JSON generated and kept outside Git.
- `ffprobe` from FFmpeg when running the curated media importer.

## Local Environment

Copy the root environment template:

```bash
cp ../../.env.example ../../.env
```

The repository PowerShell launcher reads the ignored root `.env` into the backend process without
printing values. Docker Compose reads the same file directly. Keep secret values out of Git.

The default local database name is `minis_event_local`, which is intentionally separate from older pre-Flyway development databases.

Private Admin credential:

```text
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/outside/repo/minis-dev-firebase-admin.json
```

Public Firebase Web SDK values:

```text
FIREBASE_PROJECT_ID
FIREBASE_WEB_API_KEY
FIREBASE_WEB_AUTH_DOMAIN
FIREBASE_WEB_APP_ID
FIREBASE_WEB_STORAGE_BUCKET
```

The public web values are safe to send to the browser from the local-only endpoint. The Admin JSON is private and must never be committed, copied into YAML, copied into a Docker image, or served from static resources.

## Commands

From the repository root:

```bash
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml down
docker compose -f compose.demo.yml up -d --build
docker compose -f compose.demo.yml down
```

Commands using `down -v` are documented only under the destructive old-volume policy below.
Both Compose files create the configured application bucket idempotently after MinIO is healthy;
the backend keeps `MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET=false`.

From `backend/minis-backend`:

```bash
./mvnw spotless:apply
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress package
docker build -t minis-backend:local .
```

On bash-compatible shells, export the root `.env` before starting the backend:

```bash
set -a
. ./.env
set +a
cd backend/minis-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell equivalents, with verification run from `backend/minis-backend` and startup run
from the repository root through the environment launcher:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd --batch-mode --no-transfer-progress verify
Set-Location ..\..
.\scripts\local\start-backend.ps1
```

Run the local application only after PostgreSQL is available and `GOOGLE_APPLICATION_CREDENTIALS`
plus Firebase Web SDK environment variables are set in the root `.env`, shell, or IDE run
configuration.

The demo compose topology also requires explicit `DEMO_POSTGRES_PASSWORD`, `DEMO_MINIO_ROOT_USER`, `DEMO_MINIO_ROOT_PASSWORD`, `FIREBASE_PROJECT_ID`, and `GOOGLE_APPLICATION_CREDENTIALS` values.

Content storage values:

```text
MINIS_CONTENT_STORAGE_INTERNAL_ENDPOINT=http://localhost:9000
MINIS_CONTENT_STORAGE_PUBLIC_ENDPOINT=http://localhost:9000
MINIS_CONTENT_STORAGE_BUCKET=minis-event-media
MINIS_CONTENT_STORAGE_SIGNED_READ_LIFETIME=PT10M
MINIS_CONTENT_STORAGE_CONNECT_TIMEOUT=PT5S
MINIS_CONTENT_STORAGE_READ_TIMEOUT=PT30S
MINIS_CONTENT_STORAGE_WRITE_TIMEOUT=PT2M
MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET=false
MINIS_CONTENT_IMPORTER_FFPROBE_PATH=ffprobe
```

The demo profile uses `http://minio:9000` internally and `http://localhost:9002` as the default
browser-reachable signing endpoint. Set `MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET=true` only for
disposable local/demo importer runs where bucket creation is intended.

The backend container installs `ffprobe` from the distribution FFmpeg package. Storage endpoints,
credentials, signed-read lifetime, and HTTP timeouts are validated during backend startup.

Optional local/demo event-admin bootstrap:

```text
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=false
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=
```

When enabled only in `local` or `demo`, the comma-separated Firebase UID allowlist persists `ADMIN`
as the initial PostgreSQL role during bootstrap. Email alone is not sufficient.

Profile-specific CORS values:

```text
MINIS_LOCAL_ALLOWED_ORIGINS=http://localhost:5173
MINIS_DEMO_ALLOWED_ORIGINS=http://localhost:8088
MINIS_PROD_ALLOWED_ORIGINS=
```

The local and demo variables are intentionally separate. A demo origin must not override the local
Vite origin. CORS credentials are disabled; allowed methods and headers are bounded to the API
needs, including `Authorization`, `Content-Type`, and `X-Correlation-ID`.

## Local URLs

- API base: `http://localhost:8080`
- Diagnostics page: `http://localhost:8080/auth-test/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator readiness: `http://localhost:8080/actuator/health/readiness`
- MinIO console: `http://localhost:9001`
- Local signed media URL origin: `http://localhost:9000`
- Demo reverse-proxy health: `http://localhost:8088/healthz`
- Demo signed media URL origin: `http://localhost:9002`

## Manual Firebase Browser Test

1. Start the backend with the `local` profile.
2. Start `web/event-app` with public Firebase Web SDK values and blank `VITE_API_BASE_URL`.
3. Open the React app at `http://localhost:5173`.
4. Register a new email/password user and confirm Firebase sends a verification email.
5. Confirm the unverified password user cannot bootstrap.
6. After verification, confirm React calls `POST /api/v1/auth/bootstrap`.
7. Confirm the bootstrap response creates the local PostgreSQL account and displays backend-owned
   role/status in React.
8. Confirm `GET /api/v1/me` remains available for the authenticated account projection.
9. Confirm the new account is backend `VIEWER` and `ACTIVE`.
10. Confirm email/password sign-in, Google sign-in, password reset, and sign-out.

Creator/admin rehearsal can then verify:

1. Open `/admin` as an event admin.
2. Grant a prepared viewer the creator role with a public slug and display name.
3. Open `/creators` as a guest and confirm the active creator is public.
4. Suspend the creator and confirm the public slug is no longer visible.
5. Reactivate or revoke as needed for event preparation.

The static page at `/auth-test/index.html` is only a diagnostics launcher. It does not contain Firebase sign-in logic, token display, or credential handling.

## Curated Media Import

The importer is explicit command mode only; it is not a public seed endpoint and does not run during
normal startup.

From `backend/minis-backend` after exporting the root `.env` into the process:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.importer.enabled=true --minis.content.importer.manifest-path=<path-to-private-manifest.json> --minis.content.importer.assets-root=<path-to-private-assets-root> --minis.content.importer.run-id=<safe-run-id>"
```

Reconciliation command:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.reconciliation.enabled=true --minis.content.reconciliation.run-id=<safe-run-id>"
```

The importer validates MP4/H.264/AAC vertical video with `ffprobe`, uploads to MinIO, writes
PostgreSQL metadata, skips matching re-imports, and deletes only newly uploaded objects when database
persistence fails and no committed row owns that object. Reconciliation reads published rows in
bounded batches. See `../../docs/media-and-upload.md` for the full policy.

## Social Interaction APIs

Live creator upload is not part of this implementation. Test media still enters the system through
the accepted Phase 4 curated importer.

Relationship writes require a verified Firebase bearer token mapped to an active provisioned local
account. `VIEWER`, `CREATOR`, and `ADMIN` accounts may use normal user interactions when active.
Guests may continue reading public content, but guest viewer-state booleans are `false`. Invalid
bearer tokens are rejected by the existing authentication policy and are not treated as guests.

New social routes:

- `PUT /api/v1/content/{contentId}/like`
- `DELETE /api/v1/content/{contentId}/like`
- `PUT /api/v1/content/{contentId}/save`
- `DELETE /api/v1/content/{contentId}/save`
- `PUT /api/v1/creators/{slug}/follow`
- `DELETE /api/v1/creators/{slug}/follow`
- `GET /api/v1/me/liked-content?cursor=&size=`
- `GET /api/v1/me/saved-content?cursor=&size=`

`PUT` means ensure the relationship exists. `DELETE` means ensure it does not exist. Repeating a
request, including concurrent duplicate requests, leaves one logical relationship for likes, saves,
and follows. Relationship writes return `204 No Content` on success. New likes and saves target only
`PUBLISHED` content; new follows target only a currently public active creator identified by slug.
Self-follow is rejected with the shared conflict error. Removed or missing content cannot receive new
likes or saves, but unlike and unsave remain idempotent.

The feed, content detail, and creator-content projections now include `likeCount`, `liked`, `saved`,
and `following`. Like counts are derived from `content_likes`; no public save count is exposed.
Signed media URLs are still generated only while building responses and are never stored in social
relationship tables.

Liked and saved collections are private `/me` routes. The principal determines ownership; clients do
not pass a user ID. Collections use bounded cursor pagination ordered by relationship timestamp
descending with content ID as the stable tie breaker. Removed content is excluded from normal
personal collection projections even if a historical relationship row remains.

## Common Local Errors

- Invalid `GOOGLE_APPLICATION_CREDENTIALS`: set an absolute path to a real development Firebase Admin JSON outside the repository.
- Wrong Firebase project: make sure `FIREBASE_PROJECT_ID` matches the Admin credential and Web App.
- Email/Password provider disabled: enable it in Firebase Authentication.
- Google provider disabled or unauthorized domain: enable Google in Firebase Authentication and authorize `localhost` plus the event/demo domain.
- Email not verified: send the verification email, verify, reload the Firebase user, then request a fresh token.
- Local React NETWORK error: keep `VITE_API_BASE_URL` blank, run Vite on `http://localhost:5173`, confirm the backend is reachable at `http://localhost:8080`, and confirm `MINIS_LOCAL_ALLOWED_ORIGINS` is exactly the React origin.
- PostgreSQL unavailable: run `docker compose -f compose.local.yml up -d` from the repository root.
- Environment variables not loaded: use `scripts/local/start-backend.ps1`, export the root `.env` in the shell, or configure the values in the IDE run configuration.

## Quality Gates

`./mvnw verify` runs compilation, unit tests, security tests, PostgreSQL/Testcontainers integration tests, ArchUnit rules, Spotless check, Checkstyle, PMD, SpotBugs, Maven Enforcer, and Maven dependency analysis.

Error Prone is not configured in this first slice because the local build is running on JDK 23 while the project targets Java 21, and the Maven compiler/Error Prone module-export setup would make the build less stable than the rest of the gates. SpotBugs, PMD, Checkstyle, dependency convergence, dependency analysis, and ArchUnit remain active in `verify`.

## Database Initialization

Flyway is the source of truth for the physical schema. Current migrations:

- `src/main/resources/db/migration/V1__create_identity_user_accounts.sql`
- `src/main/resources/db/migration/V2__create_creator_profiles_and_event_admin_audit.sql`
- `src/main/resources/db/migration/V3__create_content_items.sql`
- `src/main/resources/db/migration/V4__add_content_read_and_reconciliation_indexes.sql`
- `src/main/resources/db/migration/V5__create_social_interaction_relationships.sql`

- `local`: Flyway migrates the local PostgreSQL database and Hibernate validates it.
- `test`: Flyway migrates PostgreSQL Testcontainers databases and Hibernate validates them.
- `demo`: Flyway migrates the demo PostgreSQL service and Hibernate validates it.
- `prod`: Flyway migrates the configured PostgreSQL database and Hibernate validates it.

Hibernate schema generation modes `update`, `create`, and `create-drop` are not used by application profiles. Flyway `baselineOnMigrate` is not enabled globally, and Flyway clean is disabled during application startup.

### Old-Volume Policy

Normal local and demo shutdown commands preserve named PostgreSQL and MinIO volumes:

```bash
docker compose -f compose.local.yml down
docker compose -f compose.demo.yml down
```

The current schema expects a dedicated clean event database or a database already valid through the
previous Flyway migrations. Unknown Hibernate-created schemas are not adopted
silently. The following local reset is destructive and must be used only for disposable data:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

The following demo reset is also destructive:

```bash
docker compose -f compose.demo.yml down -v
docker compose -f compose.demo.yml up -d --build
```

Preserving an old database requires a separate backup, migration rehearsal, and Flyway baseline decision before pointing the application at it.

## Environment Assumptions

- Docker Desktop is available for Testcontainers and local PostgreSQL/MinIO.
- Firebase Admin credentials are stored outside Git and referenced with `GOOGLE_APPLICATION_CREDENTIALS`.
- Public Firebase Web SDK values are supplied through environment variables for the local auth test page.
- No production secrets are stored in `.env.example`, YAML files, tests, static resources, or Docker images.

## Local Pre-Push Hook

Enable:

```bash
git config core.hooksPath .githooks
```

Disable:

```bash
git config --unset core.hooksPath
```

Git hooks are local and are not enabled automatically after cloning. A developer can bypass them with `--no-verify`. GitHub Actions is therefore the authoritative shared quality gate. CI runs after code is pushed; it does not erase or prevent the remote branch push.

## GitHub Branch Protection

Repository maintainers still need to configure this manually in GitHub:

- Require pull requests before merging into protected production branches.
- Disable direct pushes to the protected production branch where appropriate.
- Require the `Backend quality gate` status check.
- Require branches to be up to date before merge if that fits the team workflow.
- Do not allow bypassing required status checks except for explicitly trusted administrators.
