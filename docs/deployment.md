# Deployment

## Demo Topology

`compose.demo.yml` starts:

- PostgreSQL with a persistent named volume.
- MinIO with a persistent named volume.
- A one-shot MinIO initializer that idempotently creates the configured application bucket.
- The Spring Boot backend using the `demo` profile.
- NGINX as the public event entry point, with the React event app built into the image.

The reverse proxy serves `/api/` to Spring Boot, exposes backend readiness at `/healthz`, preserves or generates `X-Correlation-ID`, forwards host/protocol headers, accepts a bounded 205 MB multipart request, uses 120-second body/proxy transfer timeouts, serves React at `/`, and falls back unknown frontend routes to the SPA shell.

Signed media URLs are generated for the browser-reachable MinIO origin, `http://localhost:9002` by
default. NGINX does not path-prefix or rewrite signed media URLs.

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
MINIS_CONTENT_STORAGE_BUCKET=minis-event-media
MINIS_CONTENT_STORAGE_SIGNED_READ_LIFETIME=PT10M
MINIS_CONTENT_STORAGE_CONNECT_TIMEOUT=PT5S
MINIS_CONTENT_STORAGE_READ_TIMEOUT=PT30S
MINIS_CONTENT_STORAGE_WRITE_TIMEOUT=PT2M
MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET=false
MINIS_CONTENT_UPLOAD_MAX_FILE_SIZE=200MB
MINIS_CONTENT_UPLOAD_MAX_REQUEST_SIZE=205MB
MINIS_CONTENT_UPLOAD_TEMP_DIR=
MINIS_CONTENT_UPLOAD_STALE_AGE=PT30M
MINIS_DEMO_ALLOWED_ORIGINS=http://localhost:8088
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=false
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```

Demo serves React and `/api/` under one public NGINX origin, `http://localhost:8088` by default. NGINX forwards normal request headers upstream, so the backend demo profile explicitly allows `MINIS_DEMO_ALLOWED_ORIGINS`. Do not reuse the local CORS variable for demo, and do not use wildcard credentialed CORS.
Event-admin bootstrap is local/demo only, disabled by default, and accepts Firebase UIDs only.

The demo backend uses `http://minio:9000` internally and signs public media URLs for
`http://localhost:${DEMO_MINIO_API_PORT}`. Do not return `minio:9000` to browsers.

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

MinIO object API for browser media playback is at `http://localhost:9002` by default. The console is
at `http://localhost:9003`.

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
- a prepared active creator can use `/upload` to publish an accepted vertical MP4 and reach it from the feed plus public creator content pages.
- My Uploads presents published and deliberately failed attempts only to that creator; a second creator cannot read them.
- authenticated active users can like, save, and follow through the React product surfaces after content exists.
- guest feed responses include `likeCount`, `liked=false`, `saved=false`, and `following=false`.
- browser media URLs contain the browser-reachable MinIO origin, not Docker service names.
- Range requests against a signed media URL return `206 Partial Content`.

Do not store prepared-account passwords or Firebase Admin credentials in Git, screenshots, docs, frontend bundles, or static diagnostics.

The demo flow deliberately remains server-mediated and provider-neutral. Direct browser-to-object-store upload, resumable upload, provider-specific multipart orchestration, transcoding, thumbnails, subtitles, CDN migration, and media-processing workers require a separate production R&D decision.

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

## Golden Media Snapshot

After final media import, create a PostgreSQL backup and MinIO bucket mirror before the event. Record
Flyway version, manifest identifier, media-policy ID, PUBLISHED count, REMOVED count, object count,
and asset checksums. Restore PostgreSQL first, restore the MinIO bucket second, then run content
reconciliation and verify feed playback. The detailed procedure is in `media-and-upload.md`.
