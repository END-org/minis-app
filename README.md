# MINIS

MINIS is a mobile-first platform for vertical short films and useful mini-content.

## Current Repository Truth

- Backend: Spring Boot modular monolith in `backend/minis-backend`.
- Java target: 21. The current local toolchain may run newer JDKs while Maven compiles with release 21.
- Current implemented slice: Firebase ID-token verification boundary, public Firebase email/password registration, email verification, email/password sign-in, Google sign-in, password reset, local MINIS user bootstrap, `/api/v1/auth/bootstrap`, `/api/v1/me`, creator identity, minimal event-admin preparation APIs, curated media import, MinIO-backed signed media read URLs, public categories/feed/content APIs, public creator content, ADMIN-only content removal, backend likes/saves/creator follows, private liked/saved content collections, authenticated viewer social state, React event feed, shared API errors, correlation IDs, OpenAPI configuration, and security tests.
- Current clients: React event app in `web/event-app`; placeholders under `mobile/flutter` and `web/admin` are not present in this checkout.
- Local Docker services: PostgreSQL and MinIO in `compose.local.yml`.
- Demo deployment skeleton: PostgreSQL, MinIO, Spring Boot backend, and NGINX reverse proxy in `compose.demo.yml`.
- Database schema: Flyway migrations V1 through V5; Hibernate validates the migrated schema.
- Deferred in this baseline: live media upload, transcoding, thumbnails, comments, notifications, reports, analytics, chat, frontend social UI, and complete user-management features.

## Quick Local Backend Start

Copy local environment placeholders, then start PostgreSQL and MinIO:

```bash
cp .env.example .env
docker compose -f compose.local.yml up -d
set -a
. ./.env
set +a
cd backend/minis-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose -f compose.local.yml up -d
.\scripts\local\start-backend.ps1
```

Local startup requires a Firebase development project and `GOOGLE_APPLICATION_CREDENTIALS` pointing to a Firebase Admin JSON outside the repository.

Fill the blank PostgreSQL, MinIO, Firebase, and credential-path values before startup. Keep the
profile-specific storage endpoint overrides commented unless deliberately replacing the YAML
defaults: host-local Spring uses `localhost`, while the demo backend container uses the `minio`
service hostname. Compose creates the configured MinIO bucket idempotently; the backend itself keeps
automatic bucket creation disabled.

Start the React event app after the backend is running:

```bash
cd web/event-app
npm ci
npm run dev
```

The React app uses `http://localhost:5173` with `strictPort`. Leave `VITE_API_BASE_URL` blank for local development so `/api` requests go through the Vite proxy to `http://localhost:8080`. The PowerShell launcher loads the ignored root `.env` into the backend process without printing values; Docker Compose reads the same file directly. If you deliberately use a different React origin, update `MINIS_LOCAL_ALLOWED_ORIGINS` for the backend local profile.

Curated event media uses MinIO through a read-only public path. Local signed media URLs default to
`http://localhost:9000`; demo signed media URLs default to `http://localhost:9002`. Import requires
`ffprobe` on PATH and a private, untracked manifest. See `docs/media-and-upload.md`.

Flyway upgrades an existing valid V1-V4 event database to V5 without deleting local data. Stop the
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

See `AGENTS.md` for repository-local engineering rules, `docs/getting-started.md` for local setup, `docs/authentication.md` for role/account policy, `docs/media-and-upload.md` for the media read path, `docs/deployment.md` for the demo topology, `backend/minis-backend/README.md` for backend setup, and `web/event-app/README.md` for the React event client.
