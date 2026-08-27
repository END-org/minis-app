# Deployment Baseline

## Current Environments

Local development uses `compose.local.yml` for PostgreSQL and MinIO, then runs the Spring Boot backend with the `local` profile and the React event app through Vite. The backend loads root `.env` values through `spring-dotenv`.

Demo uses `compose.demo.yml` with PostgreSQL, MinIO, the Spring Boot backend in `demo` profile, and NGINX serving the built React event app plus proxying `/api/`, `/healthz`, `/auth-test/`, `/swagger-ui/`, and `/v3/api-docs`.

Staging is not implemented. Production has `backend/minis-backend/src/main/resources/application-prod.yml`, but no production topology, TLS, backup, monitoring, alerting, rollback, or deployment automation is implemented in this repository.

`auth-test` and `/api/v1/dev/firebase-web-config` are `LOCAL_OR_DEMO_ONLY`; production-profile security tests deny them.

## Environment Matrix

| Capability | Local | Demo | Staging | Production | Notes |
|---|---|---|---|---|---|
| PostgreSQL | compose service | compose service | UNKNOWN | Profile config only | Production hosting and backup model undecided. |
| MinIO/object storage | compose service | compose service | UNKNOWN | Profile config only | Production storage provider and bucket operations undecided. |
| Spring Boot backend | local process | compose service | UNKNOWN | Dockerfile/profile only | No production deployment pipeline. |
| React event app | Vite dev server | NGINX image build | UNKNOWN | UNKNOWN | No production web hosting strategy beyond demo image. |
| NGINX reverse proxy | Not used by default | IMPLEMENTED | UNKNOWN | UNKNOWN | Demo-only config, no TLS. |
| Health checks | Actuator health/readiness | Actuator plus `/healthz` proxy | UNKNOWN | Profile exposes health/info | No external monitoring or alerts. |
| TLS | None | None | UNKNOWN | UNKNOWN | Must be decided before public traffic. |
| Environment variables | `.env` and shell | shell/untracked `.env` | UNKNOWN | `backend/minis-backend/src/main/resources/application-prod.yml` expects env | Names exist; values must stay out of Git. |
| Secrets handling | ignored `.env`; external Firebase JSON | bind-mounted Firebase JSON; env secrets | UNKNOWN | UNKNOWN | No production secret manager selected. |
| Backups | Disposable reset docs | Golden media snapshot guidance | UNKNOWN | UNKNOWN | Manual guidance only; no automated backups. |
| Logging | correlation ID and debug local logs | structured config in demo profile | UNKNOWN | structured config in prod profile | No log aggregation documented. |
| Monitoring | Actuator only | Actuator only | UNKNOWN | UNKNOWN | No metrics dashboards or alerts. |
| Deployment automation | none | `docker compose -f compose.demo.yml up -d --build` | UNKNOWN | UNKNOWN | CI quality gates exist, but no release automation. |
| Rollback | reset disposable volumes only | compose down/up guidance | UNKNOWN | UNKNOWN | No production rollback plan. |
| Firebase Console settings | manual | manual | UNKNOWN | UNKNOWN | Authorized domains/templates/providers require manual confirmation. |
| Media import | explicit local command | explicit demo command | UNKNOWN | UNKNOWN | Curated event import only; no creator upload. |

## Environment Variables

Important names used by the current repository:

- Backend/Firebase: `FIREBASE_PROJECT_ID`, `GOOGLE_APPLICATION_CREDENTIALS`, `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_STORAGE_BUCKET`.
- Backend/PostgreSQL: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, plus local/demo `POSTGRES_*` and `DEMO_POSTGRES_*`.
- Backend/storage: `MINIS_CONTENT_STORAGE_INTERNAL_ENDPOINT`, `MINIS_CONTENT_STORAGE_PUBLIC_ENDPOINT`, `MINIS_CONTENT_STORAGE_ACCESS_KEY`, `MINIS_CONTENT_STORAGE_SECRET_KEY`, `MINIS_CONTENT_STORAGE_BUCKET`, `MINIS_CONTENT_STORAGE_SIGNED_READ_LIFETIME`, `MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET`.
- Import/reconciliation: `MINIS_CONTENT_IMPORTER_*`, `MINIS_CONTENT_RECONCILIATION_ENABLED`.
- CORS/admin bootstrap: `MINIS_LOCAL_ALLOWED_ORIGINS`, `MINIS_DEMO_ALLOWED_ORIGINS`, `MINIS_PROD_ALLOWED_ORIGINS`, `MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED`, `MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS`.
- React: `VITE_FIREBASE_*`, `VITE_API_BASE_URL`, `MINIS_EVENT_APP_DEV_*`.

Values are intentionally omitted.

## CI

`.github/workflows/ci.yml` defines backend and frontend quality gates for pushes and pull requests to `main`, `master`, and `develop`. It runs backend Maven `verify`, frontend `npm ci`, lint, typecheck, tests, and build with CI Firebase placeholder values. CI is a quality gate, not deployment automation.
