# Getting Started

## Prerequisites

- JDK 21 or newer with the backend compiled for Java 21.
- Node.js 24 LTS. The repository declares this in `.nvmrc`.
- Docker Desktop or another Docker engine compatible with Testcontainers.
- Firebase development project with Email/Password and Google providers enabled.
- Firebase Admin service-account JSON stored outside the repository.

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

The default local PostgreSQL database is `minis_event_local`. MinIO is available at `http://localhost:9001`.

## Backend Startup

Set `FIREBASE_PROJECT_ID` and `GOOGLE_APPLICATION_CREDENTIALS` in the shell or IDE. The credential path must point to a private JSON file outside Git.

From `backend/minis-backend`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Useful local URLs:

- Backend health: `http://localhost:8080/actuator/health`
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

The backend includes `spring-dotenv`, so root `.env` values are visible to Spring Boot as well as Docker Compose. Local CORS defaults to `http://localhost:5173` through `MINIS_LOCAL_ALLOWED_ORIGINS`.

If you deliberately change the React host or port, set `MINIS_EVENT_APP_DEV_HOST`, `MINIS_EVENT_APP_DEV_PORT`, and backend `MINIS_LOCAL_ALLOWED_ORIGINS` to the exact origin. Do not use wildcard credentialed CORS.

Prepared-account passwords and manual test credentials belong only in a private password manager or manual test notes outside Git.

Optional event-admin bootstrap is disabled by default. For local or demo rehearsal only, set a
comma-separated Firebase UID allowlist before the first bootstrap of the prepared admin account:

```text
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=true
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=<firebase-uid-1>,<firebase-uid-2>
```

Do not use email addresses for this allowlist, and do not enable it outside `local` or `demo`.

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

Flyway creates the identity, creator-profile, and event-admin audit schema on a clean PostgreSQL database. Hibernate only validates the migrated schema.

Unknown older Hibernate-created schemas are not adopted automatically. For disposable local data, reset both PostgreSQL and MinIO volumes explicitly:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

Preserving an old database requires a separate backup and Flyway baseline rehearsal before changing application configuration.
