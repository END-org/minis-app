# Deployment

## Demo Topology

`compose.demo.yml` starts:

- PostgreSQL with a persistent named volume.
- MinIO with a persistent named volume.
- The Spring Boot backend using the `demo` profile.
- NGINX as the public event entry point, with the React event app built into the image.

The reverse proxy serves `/api/` to Spring Boot, exposes backend readiness at `/healthz`, preserves or generates `X-Correlation-ID`, forwards host/protocol headers, sets request-size and timeout defaults, serves React at `/`, and falls back unknown frontend routes to the SPA shell.

## Required Environment Variables

Set these values in the shell or a local untracked `.env` file before starting demo compose:

```text
DEMO_POSTGRES_PASSWORD
DEMO_MINIO_ROOT_USER
DEMO_MINIO_ROOT_PASSWORD
FIREBASE_PROJECT_ID
FIREBASE_WEB_API_KEY
FIREBASE_WEB_AUTH_DOMAIN
FIREBASE_WEB_APP_ID
FIREBASE_WEB_STORAGE_BUCKET
GOOGLE_APPLICATION_CREDENTIALS
```

Optional demo values:

```text
DEMO_POSTGRES_DB=minis_event_demo
DEMO_POSTGRES_USER=minis_demo
DEMO_POSTGRES_PORT=5434
DEMO_MINIO_API_PORT=9002
DEMO_MINIO_CONSOLE_PORT=9003
DEMO_HTTP_PORT=8088
MINIS_DEMO_ALLOWED_ORIGINS=http://localhost:8088
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=false
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=
```

Demo serves React and `/api/` under one public NGINX origin, `http://localhost:8088` by default. NGINX forwards normal request headers upstream, so the backend demo profile explicitly allows `MINIS_DEMO_ALLOWED_ORIGINS`. Do not reuse the local CORS variable for demo, and do not use wildcard credentialed CORS.
Event-admin bootstrap is local/demo only, disabled by default, and accepts Firebase UIDs only.

## Startup

From the repository root:

```bash
docker compose -f compose.demo.yml up -d --build
```

Health through the public reverse-proxy path:

```bash
curl -i http://localhost:8088/healthz
```

The React event app is at `http://localhost:8088/`. The backend diagnostic launcher is at `http://localhost:8088/auth-test/index.html`.

Nested frontend routes, such as `http://localhost:8088/account`, refresh through NGINX and return the React shell. Unknown `/api/` routes are proxied to Spring Boot and do not return React HTML.

## Manual Event Authentication

Manual rehearsal requires Firebase Email/Password and Google providers enabled for the event project, authorized localhost/demo domains, and backend Firebase Admin credentials for the same project. Verify at least:

- guest can open the React event app;
- public `/creators` loads through the real backend and shows an empty state when no creators exist;
- new email/password registration sends a verification email;
- unverified password users remain on `/verify-email` and cannot bootstrap;
- verified password users bootstrap as backend `VIEWER` accounts;
- email/password sign-in;
- Google sign-in;
- password-reset email request;
- prepared creator account if already configured;
- prepared admin account if already configured;
- sign-out clears authenticated application state;
- 401 session expiry returns to sign-in;
- 403 local-account rejection shows forbidden access without signing out.
- event admin can grant creator capability, verify the creator, suspend/reactivate an account, and
  revoke creator capability without deleting the creator profile.
- public creator routes show only active accounts with the current backend `CREATOR` role.

Do not store prepared-account passwords or Firebase Admin credentials in Git, screenshots, docs, frontend bundles, or static diagnostics.

Firebase Console settings requiring manual confirmation for each event environment:

- Email/Password provider enabled.
- Google provider enabled.
- Localhost and the event/demo domain authorized for Firebase Authentication.
- Firebase Web App config matches the public `VITE_FIREBASE_*` values.
- Firebase Admin project ID and service account match the Web App project.
- Verification-email and password-reset templates/action URLs are appropriate for the event domain.

## Shutdown and Disposable Reset

Stop without deleting data:

```bash
docker compose -f compose.demo.yml down
```

Recreate disposable demo PostgreSQL and MinIO volumes:

```bash
docker compose -f compose.demo.yml down -v
docker compose -f compose.demo.yml up -d --build
```

## Old-Volume Policy

The event database must be a dedicated clean database. Flyway `baselineOnMigrate` is not enabled globally, and application startup does not silently adopt unknown Hibernate-created schemas.

Preserving an old database requires a separate backup, restore rehearsal, schema comparison, and explicit Flyway baseline decision before deployment.
