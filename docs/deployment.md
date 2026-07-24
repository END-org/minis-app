# Deployment

## Demo Topology

`compose.demo.yml` starts:

- PostgreSQL with a persistent named volume.
- MinIO with a persistent named volume.
- The Spring Boot backend using the `demo` profile.
- NGINX as the public event entry point.

The reverse proxy serves `/api/` to Spring Boot, exposes backend readiness at `/healthz`, preserves or generates `X-Correlation-ID`, forwards host/protocol headers, sets request-size and timeout defaults, and serves a temporary diagnostic page at `/` until the React event client exists.

## Required Environment Variables

Set these values in the shell or a local untracked `.env` file before starting demo compose:

```text
DEMO_POSTGRES_PASSWORD
DEMO_MINIO_ROOT_USER
DEMO_MINIO_ROOT_PASSWORD
FIREBASE_PROJECT_ID
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
MINIS_ALLOWED_ORIGINS=
```

An empty `MINIS_ALLOWED_ORIGINS` keeps the browser path same-origin through NGINX. Do not use wildcard credentialed CORS.

## Startup

From the repository root:

```bash
docker compose -f compose.demo.yml up -d --build
```

Health through the public reverse-proxy path:

```bash
curl -i http://localhost:8088/healthz
```

The temporary diagnostic page is at `http://localhost:8088/`.

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
