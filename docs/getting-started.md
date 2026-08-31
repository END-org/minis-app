# Getting Started

## Prerequisites

- JDK 21 or newer with the backend compiled for Java 21.
- Node.js 24 LTS. The repository declares this in `.nvmrc`.
- Docker Desktop or another Docker engine compatible with Testcontainers.
- Firebase development project with Email/Password and Google providers enabled.
- Firebase Admin service-account JSON stored outside the repository.
- `ffprobe` from FFmpeg for curated media import.

## Local Infrastructure

Copy the root environment template and fill only local values:

```bash
cp .env.example .env
docker compose -f compose.local.yml up -d
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose -f compose.local.yml up -d
```

The default local PostgreSQL database is `minis_event_local`. MinIO is available at
`http://localhost:9000` for object API access and `http://localhost:9001` for the console.
Compose creates the configured application bucket idempotently after MinIO is healthy. The backend
does not create buckets during normal startup.

Fill `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD`, Firebase Web values, and
`GOOGLE_APPLICATION_CREDENTIALS` before starting the backend. Keep
`MINIS_CONTENT_STORAGE_INTERNAL_ENDPOINT` and `MINIS_CONTENT_STORAGE_PUBLIC_ENDPOINT` unset unless
you intentionally need to override the profile defaults. Host-local Spring uses `localhost`; the
demo backend container uses the Docker `minio` service hostname internally.

Validate and inspect the non-destructive local infrastructure flow with:

```powershell
docker compose -f compose.local.yml config
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml ps
```

## Backend Startup

Set `FIREBASE_PROJECT_ID` and `GOOGLE_APPLICATION_CREDENTIALS` in the root `.env`, shell, or IDE.
The credential path must point to a private JSON file outside Git.

From a bash-compatible shell at the repository root:

```bash
set -a
. ./.env
set +a
cd backend/minis-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell:

```powershell
.\scripts\local\start-backend.ps1
```

Useful local URLs:

- Backend health: `http://localhost:8080/actuator/health`
- Backend liveness: `http://localhost:8080/actuator/health/liveness`
- Backend readiness: `http://localhost:8080/actuator/health/readiness`
- Auth test page: `http://localhost:8080/auth-test/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## React Event App

Copy the frontend environment template and fill public Firebase Web SDK values:

```bash
cd web/event-app
cp .env.example .env.local
npm ci
npm run dev
```

Windows PowerShell:

```powershell
Set-Location web/event-app
Copy-Item .env.example .env.local
npm ci
npm run dev
```

The local app runs at `http://localhost:5173` and uses Vite `strictPort`, so a port collision fails instead of silently moving to `5174`. Leave `VITE_API_BASE_URL` blank for local development; `/api` and `/actuator` requests are proxied by Vite to `MINIS_EVENT_APP_DEV_PROXY_TARGET`, which defaults to `http://localhost:8080`.

Expected local browser/backend values:

- React origin: `http://localhost:5173`
- Backend URL: `http://localhost:8080`
- API base URL in React: blank, meaning same-origin `/api` through the Vite proxy

The PowerShell launcher loads the ignored root `.env` into the backend process without printing
values; Docker Compose reads the same file directly. Local CORS defaults to
`http://localhost:5173` through `MINIS_LOCAL_ALLOWED_ORIGINS`.

If you deliberately change the React host or port, set `MINIS_EVENT_APP_DEV_HOST`, `MINIS_EVENT_APP_DEV_PORT`, and backend `MINIS_LOCAL_ALLOWED_ORIGINS` to the exact origin. Do not use wildcard credentialed CORS.

Prepared-account passwords and manual test credentials belong only in a private password manager or manual test notes outside Git.

Optional event-admin bootstrap is disabled by default. For local or demo rehearsal only, set a
comma-separated Firebase UID allowlist before the first bootstrap of the prepared admin account:

```text
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=true
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=<firebase-uid-1>,<firebase-uid-2>
```

Do not use email addresses for this allowlist, and do not enable it outside `local` or `demo`.

## Curated Media Import

The event feed is populated by a private untracked manifest and local/demo MinIO. The importer
requires `ffprobe` and only accepts the bounded event MP4/H.264/AAC vertical profile described in
`media-and-upload.md`.

From `backend/minis-backend`, after PostgreSQL and MinIO are healthy and the root `.env` has been
exported into the process:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.importer.enabled=true --minis.content.importer.manifest-path=<path-to-private-manifest.json> --minis.content.importer.assets-root=<path-to-private-assets-root> --minis.content.importer.run-id=<safe-run-id>"
```

Run reconciliation with:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.reconciliation.enabled=true --minis.content.reconciliation.run-id=<safe-run-id>"
```

Local signed media URLs default to `http://localhost:9000`. They must not contain Docker-internal
hostnames such as `minio:9000`.

Storage defaults are a 10-minute signed-read lifetime, 5-second connect timeout, 30-second read
timeout, and 2-minute write timeout. They are configurable through the matching
`MINIS_CONTENT_STORAGE_*` variables in `.env.example`. Duration values use ISO-8601 syntax such as
`PT5S`; invalid, zero, or sub-unit values fail startup validation.

## Verification

From `backend/minis-backend`:

```bash
./mvnw spotless:apply
./mvnw --batch-mode --no-transfer-progress verify
```

From `web/event-app`:

```bash
npm ci
npm run lint
npm run typecheck
npm run test
npm run build
```

## Flyway and Old Volumes

Flyway creates and upgrades the identity, creator-profile, event-admin audit, and content metadata
schema. A valid V1-V3 local database upgrades automatically to V4, after which Hibernate validates
the migrated schema. Existing rows and named Docker volumes are preserved.

After backend startup, inspect migration history without changing it:

```powershell
docker compose -f compose.local.yml exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"'
```

Stop the normal local environment without deleting data:

```powershell
docker compose -f compose.local.yml down
```

Unknown older Hibernate-created schemas are not adopted automatically. The following command is
destructive and deletes both local PostgreSQL and MinIO volumes; use it only for disposable data:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

Preserving an old database requires a separate backup and Flyway baseline rehearsal before changing application configuration.
