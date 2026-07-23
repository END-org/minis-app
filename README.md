# MINIS

MINIS is a mobile-first platform for vertical short films and useful mini-content.

## Current Repository Truth

- Backend: Spring Boot modular monolith in `backend/minis-backend`.
- Java target: 21. The current local toolchain may run newer JDKs while Maven compiles with release 21.
- Current implemented slice: Firebase ID-token verification boundary, local MINIS user bootstrap, `/api/v1/auth/bootstrap`, `/api/v1/me`, shared API errors, correlation IDs, OpenAPI configuration, and security tests.
- Placeholder clients: `mobile/flutter` and `web/admin`.
- Local Docker services: PostgreSQL and MinIO in `compose.local.yml`.
- Deferred in this baseline: React event client, Flyway migrations, media upload, feed/content, social interactions, notifications, reports, analytics, chat, and admin product features.

## Quick Local Backend Start

Copy local environment placeholders, then start PostgreSQL and MinIO:

```bash
cp .env.example .env
docker compose -f compose.local.yml up -d
cd backend/minis-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose -f compose.local.yml up -d
Set-Location backend/minis-backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Local startup requires a Firebase development project and `GOOGLE_APPLICATION_CREDENTIALS` pointing to a Firebase Admin JSON outside the repository.

## Verification

From `backend/minis-backend`:

```bash
./mvnw spotless:apply
./mvnw --batch-mode --no-transfer-progress verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

See `AGENTS.md` for repository-local engineering rules and `backend/minis-backend/README.md` for backend setup, Firebase auth testing, local URLs, and troubleshooting.
