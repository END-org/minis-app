# MINIS

MINIS is a mobile-first platform for vertical short films and useful mini-content.

## Current Repository Truth

- Backend: Spring Boot modular monolith in `backend/minis-backend`.
- Java target: 21. The current local toolchain may run newer JDKs while Maven compiles with release 21.
- Current implemented slice: Firebase ID-token verification boundary, public Firebase email/password registration, email verification, email/password sign-in, Google sign-in, password reset, local MINIS user bootstrap, `/api/v1/auth/bootstrap`, `/api/v1/me`, creator identity, minimal event-admin preparation APIs, controlled creator-only multipart video upload, curated media import fallback, MinIO-backed signed media read URLs, public categories/feed/content APIs, public creator content, ADMIN-only content removal, backend likes/saves/creator follows, private liked/saved content collections, authenticated viewer social state, React upload and social product flows, shared API errors, correlation IDs, OpenAPI configuration, and security tests.
- Current clients: React event app in `web/event-app`; placeholders under `mobile/flutter` and `web/admin` are not present in this checkout.
- Local Docker services: PostgreSQL and MinIO in `compose.local.yml`.
- Demo deployment skeleton: PostgreSQL, MinIO, Spring Boot backend, and NGINX reverse proxy in `compose.demo.yml`.
- Database schema: Flyway migrations V1 through V6; Hibernate validates the migrated schema.
- Deferred: transcoding, thumbnails, comments, notifications, reports, analytics, chat, direct-to-cloud/resumable uploads, CDN/provider migration, and complete user-management features.

## Quick Local Product Start

The full-stack helper is for Windows PowerShell:

```powershell
Copy-Item .env.example .env
.\scripts\local\start-dev.ps1
```

Local startup requires a Firebase development project and `GOOGLE_APPLICATION_CREDENTIALS` pointing to a Firebase Admin JSON outside the repository.

Fill the blank PostgreSQL, MinIO, Firebase, and credential-path values before startup. Keep the
profile-specific storage endpoint overrides commented unless deliberately replacing the YAML
defaults: host-local Spring uses `localhost`, while the demo backend container uses the `minio`
service hostname. Compose creates the configured MinIO bucket idempotently; the backend itself keeps
automatic bucket creation disabled.

`start-dev.ps1` validates Compose, starts/reuses PostgreSQL and MinIO, initializes the bucket, starts the backend and React app on their strict configured ports, and waits for readiness. It stores only helper-owned PID records and logs in ignored `.minis-local/`. Use `./scripts/local/status-dev.ps1` to inspect the local stack and `./scripts/local/stop-dev.ps1` to stop helper-owned processes plus Compose without deleting named volumes.

The React app uses `http://localhost:5173` with `strictPort`. Leave `VITE_API_BASE_URL` blank for local development so `/api` requests go through the Vite proxy to `http://localhost:8080`. The PowerShell helper loads the ignored root `.env` into its child processes without printing values; Docker Compose reads the same file directly. If you deliberately use a different React origin, update `MINIS_LOCAL_ALLOWED_ORIGINS` for the backend local profile.

Creators normally publish event video through the React Upload screen. The backend spools an authenticated multipart MP4 to a server-owned temporary path, validates the accepted media profile with `ffprobe`, stores it privately in MinIO, and publishes it after a short PostgreSQL transition. The Phase 4 curated importer remains available for recovery/demo media, requires `ffprobe` on PATH and a private untracked manifest, and is not the normal browser workflow. Local signed media URLs default to `http://localhost:9000`; demo signed media URLs default to `http://localhost:9002`. See `docs/media-and-upload.md`.

Flyway upgrades an existing valid V1-V5 event database to V6 without deleting local data. Stop the
normal local environment with `docker compose -f compose.local.yml down`. The following reset is
destructive and must be used only for intentionally disposable data:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

## Demo Deployment Entry Point

The demo topology is production-like and requires explicit secret-bearing environment values:

```bash
docker compose -f compose.demo.yml up -d --build
```

Backend readiness is exposed through the reverse proxy at `http://localhost:8088/healthz`.

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

Windows PowerShell:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

See `AGENTS.md` for repository-local engineering rules, `docs/getting-started.md` for local setup, `docs/authentication.md` for role/account policy, `docs/media-and-upload.md` for the media path, `docs/api-handoff.md` for API behavior, `docs/deployment.md` for the demo topology, `backend/minis-backend/README.md` for backend setup, and `web/event-app/README.md` for the React event client.
